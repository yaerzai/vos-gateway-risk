package com.ytl.vos.gateway.risk.service.impl;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.service.SftpBaseService;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.db.model.PageData;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.vos.gateway.risk.constant.RedisKeysConstant;
import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.enums.AliAsrStatusCodeEnum;
import com.ytl.vos.gateway.risk.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.risk.runner.UpdateAsrResultQueue;
import com.ytl.vos.gateway.risk.service.AsrRecognitionService;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.NlpService;
import com.ytl.vos.gateway.risk.service.bo.HttpSendRequestBO;
import com.ytl.vos.gateway.risk.service.bo.HttpSendResponseBO;
import com.ytl.vos.gateway.risk.utils.FileTransUtil;
import com.ytl.vos.persistence.dataservice.AsrProjectParamConfigDataService;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectParamConfigDataBO;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectParamConfigQueryBO;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;
import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import com.ytl.vos.persistence.enums.UploadPathEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

/**
 * @author kf-zhanghui
 * @date 2023/7/25 18:17
 */

@Service("aliYunRecognizeService")
@Slf4j
public class AliYunAsrRecognitionServiceImpl implements AsrRecognitionService {

    /**
     * ASR识别成功状态码。
     */
    private static final String ASR_ALI_SUCCESS = "21050000";
    /**
     * 启用
     */
    private static final String ASR_ALI_PARAM_ENABLE_STATUS = "1";
    /**
     * 关闭
     */
    private static final String ASR_ALI_PARAM_DISABLE_STATUS = "0";
    @Resource
    private UpdateAsrResultQueue updateAsrResultQueue;
    @Resource
    private NlpService nlpService;
    @Resource
    private DataService dataService;
    @Resource
    private SftpBaseService sftpBaseService;
    @Resource
    private RedisCacheService redisCacheService;
    @Override
    public HttpSendResponseBO send(HttpSendRequestBO httpSendRequestBO) {
        if(StrUtil.isEmpty(httpSendRequestBO.getAccessKeyId())){
            throw new BusinessException(VosErrCodeEnum.ASR_ACCESS_KEY_ID_BLANK);
        }
        if(StrUtil.isEmpty(httpSendRequestBO.getAccessKeySecret())){
            throw new BusinessException(VosErrCodeEnum.ASR_ACCESS_KEY_SECRET_BLANK);
        }
        if(StrUtil.isEmpty(httpSendRequestBO.getAppKey())){
            throw new BusinessException(VosErrCodeEnum.ASR_APP_KEY_BLANK);
        }
        if(StrUtil.isEmpty(httpSendRequestBO.getCallbackUrl())){
            throw new BusinessException(VosErrCodeEnum.ASR_ALI_CALLBACK_URL_BLANK);
        }
        if(StrUtil.isEmpty(httpSendRequestBO.getVoiceAddress())){
            throw new BusinessException(VosErrCodeEnum.ASR_VOICE_URL_BLANK);
        }
        //通过动态添加AliYun识别接口参数
        JSONObject paramObject = new JSONObject();
        addAliYunParams(paramObject,httpSendRequestBO);
        //获取录音文件路径
        FileTransUtil client = new FileTransUtil(httpSendRequestBO.getAccessKeyId(), httpSendRequestBO.getAccessKeySecret());
        return client.submitFileTransRequest(httpSendRequestBO.getAppKey(), httpSendRequestBO.getVoiceAddress(),httpSendRequestBO.getCallbackUrl(),paramObject);
    }


    @Override
    public void callback(String body) {
        log.warn("[ASR-ALi callback] 阿里云ASR回调请求 body: " + body);
        try {
            // 获取JSON格式的文件识别结果。
            String result = body;
            JSONObject jsonResult = JSONObject.parseObject(result);
            // 解析并输出相关结果内容。
            CallRiskProcessDataBO callRiskProcessDataBO = new CallRiskProcessDataBO();
            String taskId = jsonResult.getString(FileTransUtil.KEY_TASK_ID);
            if(StrUtil.isEmpty(taskId)){
                log.warn("[ASR-ALi callback] 阿里云ASR回调请求TaskId为空");
                throw new BusinessException("TaskId不能空");
            }
            callRiskProcessDataBO.setTaskId(taskId);
            String manualAsr = redisCacheService.getHash(RedisKeysConstant.MANUAL_ASR_TABLE_TIME, taskId, String.class);
            if(StrUtil.isEmpty(manualAsr)){
                //为空代表自动识别ASR,分表时间为当天
                callRiskProcessDataBO.setTableTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD));
            }else {
                //不为空代表时手动ASR,分表时间从Redis中获取
                log.warn("[ASR-ALi callback] 手动识别ASR回调TaskId:{},tableTime:{}",taskId,manualAsr);
                callRiskProcessDataBO.setTableTime(manualAsr);
            }
            String statusCode = jsonResult.getString(FileTransUtil.KEY_STATUS_CODE);
            String statusText = jsonResult.getString(FileTransUtil.KEY_STATUS_TEXT);
            log.info("[ASR-ALi callback] TaskId:{} StatusCode:{} StatusText:{}",taskId,statusCode,statusText);
            // 以2开头状态码为正常状态码，回调方式正常状态只返回“21050000”。
            if(ASR_ALI_SUCCESS.equals(statusCode)) {
                String requestTime = jsonResult.getString(FileTransUtil.KEY_REQUEST_TIME);
                String solveTime = jsonResult.getString(FileTransUtil.KEY_SOLVE_TIME);
                String bizDuration = jsonResult.getString(FileTransUtil.KEY_BIZ_DURATION);
                JSONArray jsonArray = jsonResult.getJSONObject(FileTransUtil.KEY_RESULT).getJSONArray(FileTransUtil.KEY_SENTENCES);
                log.info("[ASR-ALi callback] RequestTime:{} solveTime:{} BizDuration:{} Result.Sentences.size:{}",requestTime,solveTime,bizDuration,jsonArray.size());
                callRiskProcessDataBO.setAsrRequestTime(DateUtils.getDate(Long.valueOf(requestTime),DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS_C));
                callRiskProcessDataBO.setAsrDoneTime(DateUtils.getDate(Long.valueOf(solveTime),DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS_C));
                String resultJsonString = jsonResult.getJSONObject(FileTransUtil.KEY_RESULT).toJSONString();
                log.info("[ASR-ALi callback] ASR文本内容：{}",resultJsonString);
                if (capacityCompare64KB(resultJsonString)) {
                    //如果文本内容大于64KB,则将文本内容存入文件中,将文件地址存入asrText字段
                    String completeRemoteDir = UploadPathEnum.getCompleteRemoteDir(dataService.getSysParam(SysParamEnum.UPLOAD_FILE_DIR),
                            UploadPathEnum.RiskASRVoiceFile,DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD));
                    String fileName = taskId+ ".json";
                    log.warn("[ASR-ALi callback] taskId:{} 录音文件内容大于64KB,将文件通过sftp上传到：{}",taskId,completeRemoteDir+ File.separator + fileName);
                    boolean upload = sftpBaseService.upload(completeRemoteDir, fileName, new ByteArrayInputStream(resultJsonString.getBytes("UTF-8")));
                    if(upload){
                        callRiskProcessDataBO.setAsrText(completeRemoteDir+ File.separator + fileName);
                    }else {
                        log.error("[ASR-ALi callback] 录音文件上传到远程服务器失败：{}",completeRemoteDir+ File.separator + fileName);
                        callRiskProcessDataBO.setAsrText("放入远程服务器失败");
                    }
                }else {
                    callRiskProcessDataBO.setAsrText(resultJsonString);
                }
                //将ASR处理结果修改为成功
                callRiskProcessDataBO.setAsrStatus(AsrHandleStatusEnum.SUCCESS.getCodeId());
            }else {
                //代表不成功的ASR识别
                callRiskProcessDataBO.setAsrStatus(AsrHandleStatusEnum.FAIL.getCodeId());
                if(StrUtil.isEmpty(statusCode)){
                    callRiskProcessDataBO.setAsrErrMsg("ASR识别回调的状态码为空");
                }else {
                    AliAsrStatusCodeEnum parse = AliAsrStatusCodeEnum.parse(statusCode);
                    if(parse ==null){
                        callRiskProcessDataBO.setAsrErrMsg("阿里ASR识别失败,错误状态码：" + statusCode);
                    }else {
                        callRiskProcessDataBO.setAsrErrMsg(parse.getMsg());
                    }
                }
            }
            log.info("[ASR-ALi callback] 插入更新ASR识别结果DB队列:{}",callRiskProcessDataBO);
            updateAsrResultQueue.add(callRiskProcessDataBO);
            if(AsrHandleStatusEnum.SUCCESS.getCodeId() == callRiskProcessDataBO.getAsrStatus()){
                //异步执行NLP处理流程
                SysConstant.nlpHandle.execute(()-> nlpService.nlpAnalyze(taskId,jsonResult.getJSONObject(FileTransUtil.KEY_RESULT).getJSONArray(FileTransUtil.KEY_SENTENCES)));
            }
        } catch (Exception e) {
            log.error("[ASR-ALi callback] 解析Ali回调结果报错",e);
        }
    }


    /**
     * 判断录音文本是否大于64KB
     * @param json
     * @return
     */
    private boolean capacityCompare64KB(String json){
        if(StrUtil.isEmpty(json)){
            return false;
        }
        int capacityInKB = json.getBytes().length / 1024;
        return capacityInKB >= 64;
    }


    /**
     * 添加阿里云识别参数
     * @param paramObject
     */
    private void addAliYunParams(JSONObject paramObject,HttpSendRequestBO httpSendRequestBO) {
        List<AsrProjectParamConfigDataBO> asrProjectParamByNo = dataService.getAsrProjectParamByNo(httpSendRequestBO.getProjectNo());
        if(!CollectionUtils.isEmpty(asrProjectParamByNo)){
            for (AsrProjectParamConfigDataBO datum : asrProjectParamByNo) {
                String paramCode = datum.getParamCode();
                String paramValue = datum.getParamValue();
                if(StrUtil.isNotEmpty(paramCode) && StrUtil.isNotEmpty(paramValue)) {
                    if(FileTransUtil.KEY_ENABLE_WORDS.equals(paramCode)
                            || FileTransUtil.ENABLE_SAMPLE_RATE_ADAPTIVE.equals(paramCode)
                            || FileTransUtil.AUTO_SPLIT.equals(paramCode)
                            || FileTransUtil.ENABLE_INVERSE_TEXT_NORMALIZATION.equals(paramCode)){
                        paramObject.put(paramCode,paramValue.equals(ASR_ALI_PARAM_ENABLE_STATUS));
                    }else {
                        paramObject.put(paramCode,paramValue);
                    }
                }
            }
        }

    }
}
