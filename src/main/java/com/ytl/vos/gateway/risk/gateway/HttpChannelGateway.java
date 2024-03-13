package com.ytl.vos.gateway.risk.gateway;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytl.common.base.controller.SystemController;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.base.utils.ThreadUtils;

import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.gateway.bo.ChannelSubmitThread;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.gateway.risk.service.AsrRecognitionService;
import com.ytl.vos.gateway.risk.service.GatewayService;
import com.ytl.vos.gateway.risk.service.bo.HttpSendRequestBO;
import com.ytl.vos.gateway.risk.service.bo.HttpSendResponseBO;
import com.ytl.vos.jms.code.dto.asr.AsrAliYunDTO;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectInfoDataBO;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;
import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yuzc
 * @Description: http网关
 * @date 2019/11/22 16:36
 */
@Slf4j
@Data
public class HttpChannelGateway {

    /**
     * 项目厂商信息
     */
    private AsrProjectInfoDataBO asrProjectInfoDataBO;
    /**
     * 网关Service
     */
    private GatewayService gatewayService;
    /**
     * 录音文件识别服务
     */
    private AsrRecognitionService asrRecognitionService;

    /**
     * 提交线程
     */
    private List<ChannelSubmitThread> submitThreads = new CopyOnWriteArrayList<>();

    /**
     * 质检IP与域名映射字典
     */
    private static String  riskRtpIpMappingDomain = "RISK_RTP_IP_MAPPING_DOMAIN";

    public HttpChannelGateway(AsrProjectInfoDataBO asrProjectInfoDataBO) {
        this.asrProjectInfoDataBO = asrProjectInfoDataBO;
    }


    protected synchronized int getChannelSubmitThreadSize() {
        return submitThreads.size();
    }

    /***
     * 创建队列主线程
     */
    public void createSocketChannel() {
        createSubmitThread(asrProjectInfoDataBO);
    }

    /**
     * 创建提交线程
     */
    private synchronized void createSubmitThread(AsrProjectInfoDataBO asrProjectInfoDataBO) {
        String projectNo = asrProjectInfoDataBO.getProjectNo();
        ChannelSubmitThread channelSubmitThread = ChannelSubmitThread.builder().channelGateway(this).build();
        Runnable thread = () -> process(channelSubmitThread);
        channelSubmitThread.setThread(thread);

        SysConstant.submitExecutors.execute(thread);
        submitThreads.add(channelSubmitThread);
        log.info("[ASR Gateway Refresh] {} 新建线程",projectNo);
        gatewayService.getGatewayServiceResource().getMonitorService().recordLogin(projectNo);
    }

    /**
     * 从队列中获取数据,发送HTTP请求
     *
     * @param channelSubmitThread
     */
    private void process(ChannelSubmitThread channelSubmitThread) {
        // 每一秒开始的毫秒值
        long startMill = System.currentTimeMillis();
        // 当前秒还剩多少毫秒数，用于控制流速
        long remainMill;
        String projectNo = channelSubmitThread.getChannelGateway().getAsrProjectInfoDataBO().getProjectNo();
        AtomicInteger sleepTimes = new AtomicInteger();
        while (SystemController.getSystemOpenFlag() && GatewayManager.getChannelGateway(projectNo) != null) {
            // 获取当前毫秒值
            long millNow = System.currentTimeMillis();
            if (millNow - startMill >= 1000) {
                startMill = millNow;
            }
            // 当前秒剩余毫秒数（用于控制匀速发送）
            remainMill = 1000 - (millNow - startMill);
            // 流速控制--获取每秒提交数
            int secondSubmitNum = gatewayService.getGatewayServiceResource().getMonitorService().getSubmitNum(projectNo);
            if (asrProjectInfoDataBO.getQps() != 0 && secondSubmitNum >= asrProjectInfoDataBO.getQps()) {
                log.warn("[ASR queue] {} 超流速,不消费! limit:{} submitNum:{} sleepTime:{}",projectNo,asrProjectInfoDataBO.getQps(), secondSubmitNum, remainMill / 5);
                ThreadUtils.sleep(remainMill / 5);
                continue;
            }

            int sleepTime = gatewayService.getGatewayServiceResource().getDataService().getSysParamInt(SysParamEnum.PROCESS_THREAD_SLEEP_TIME);
            AsrAliYunDTO message = getSubmitMessage();
            //当队列无数据且休眠次数超过1500次,且该厂商主线程数量>1,则关闭该主线程
            if (message == null && sleepTimes.get() >= 1500 && gatewayService.getGatewayServiceResource().getMonitorService().getClientNum(projectNo) > 1) {
                log.warn("[ASR queue] {} 线程由于队列中无数据连续休眠{}次,线程结束!", projectNo,sleepTimes.get());
                break;
            }
            //当队列无数据,则休眠次数+1,休眠后,再次循环
            if (message == null) {
                sleepTimes.incrementAndGet();
                ThreadUtils.sleep(sleepTime);
                continue;
            }
//            log.info("message: {}", JSONUtil.toJsonStr(message));
//            // 消息消费失败次数已超限
//            int retryTime = gatewayService.getGatewayServiceResource().getDataService().getSysParamInt(SysParamEnum.CHANNEL_SUBMIT_MAX_FAIL_TIMES);
//            if (message.getConsumeFailTime().incrementAndGet() > retryTime && retryTime > 0) {
//                log.error("该消息消费失败次数已超限，message: {}", JSONUtil.toJsonStr(message));
//                continue;
//            }
            //拿到队列消息后发送HTTP请求
            HttpSendResponseBO resultData;
            HttpSendRequestBO requestBO = null;
            try {
                String recordVoiceUrl = gatewayService.getGatewayServiceResource().getDataService().getSysCodeName(riskRtpIpMappingDomain, message.getIp(), Strings.EMPTY);
                requestBO = HttpSendRequestBO.builder()
                        .callId(message.getCallId())
                        .projectNo(message.getProjectNo())
                        .accessKeyId(channelSubmitThread.getChannelGateway().getAsrProjectInfoDataBO().getUserNo())
                        .accessKeySecret(channelSubmitThread.getChannelGateway().getAsrProjectInfoDataBO().getPwd())
                        .appKey(channelSubmitThread.getChannelGateway().getAsrProjectInfoDataBO().getAppKey())
                        .callbackUrl(channelSubmitThread.getChannelGateway().getAsrProjectInfoDataBO().getCallbackUrl())
                        .build();
                if(StrUtil.isNotEmpty(message.getManualAsrFlag()) && "1".equals(message.getManualAsrFlag())){
                    requestBO.setVoiceAddress(message.getFileUrl());
                }else {
                    requestBO.setVoiceAddress(recordVoiceUrl + "/" + message.getCallId() + ".mp3");
                }
                resultData = asrRecognitionService.send(requestBO);
            } catch (Exception e) {
                //当报错时,也需要将错误信息
                log.error("阿里云识别提交异常！message: {}", JSONUtil.toJsonStr(message), e);
                resultData = HttpSendResponseBO.builder().respResult(AsrHandleStatusEnum.FAIL)
                        .msg("提交识别失败:" + e.getMessage())
                        .taskId("")
                        .requestId("")
                        .build();
            }
            //添加一条提交记录,用于流速控制
            gatewayService.getGatewayServiceResource().getMonitorService().recordSubmit(projectNo);
            if(StrUtil.isNotEmpty(message.getManualAsrFlag()) && "1".equals(message.getManualAsrFlag())){
                //走手动ASR流程
                CallRiskProcessDataBO manualDataBO = convertManualDataBO(resultData,message);
                log.warn("[manual ASR] 手动ASR结果：{}",manualDataBO);
                gatewayService.saveManualSubmit(manualDataBO);
            }else {
                //走自动ASR流程
                CallRiskProcessDataBO resultBO = convertDataBO(requestBO,resultData,message);
                gatewayService.saveChannelSubmit(resultBO);
            }
        }
        remove(channelSubmitThread);
        gatewayService.getGatewayServiceResource().getMonitorService().recordLogout(projectNo);
    }

    /**
     *手动ASR识别更新ASR表对象封装
     * @param resultData
     * @param message
     * @return
     */
    private CallRiskProcessDataBO convertManualDataBO(HttpSendResponseBO resultData, AsrAliYunDTO message) {
        CallRiskProcessDataBO resultBO = new CallRiskProcessDataBO();
        resultBO.setId(message.getAsrId());
        //分表时间
        resultBO.setTableTime(message.getTableTime());
        //resultData数据封装
        resultBO.setAsrStatus(resultData.getRespResult().getCodeId());
        resultBO.setAsrErrMsg(resultData.getMsg());
        resultBO.setTaskId(resultData.getTaskId());
        resultBO.setAsrRequestTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS_C));
        return resultBO;
    }

    /**
     * 插入数据库DataBO对象封装
     * @param resultData
     * @param message
     * @return
     */
    private CallRiskProcessDataBO convertDataBO(HttpSendRequestBO requestBO,HttpSendResponseBO resultData, AsrAliYunDTO message) {
        CallRiskProcessDataBO resultBO = new CallRiskProcessDataBO();
        //requestBO
        if(requestBO !=null){
            resultBO.setFileUrl(requestBO.getVoiceAddress());
        }
        //这是message消息数据
        resultBO.setPlatCallId(message.getPlatCallId());
        resultBO.setCallId(message.getCallId());
        resultBO.setCustomerNo(message.getCustomerNo());
        resultBO.setUserNo(message.getUserNo());
        //resultData数据封装
        resultBO.setAsrStatus(resultData.getRespResult().getCodeId());
        resultBO.setAsrErrMsg(resultData.getMsg());
        resultBO.setTaskId(resultData.getTaskId());
        //分表时间
        resultBO.setTableTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD));
        //当前请求时间
        resultBO.setAsrRequestTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS_C));
        return resultBO;
    }

    /**
     * 获取通道提交信息
     *
     * @return
     */
    private AsrAliYunDTO getSubmitMessage() {
        AliYunAsrSubmitQueue asrSubmitQueue = GatewayManager.getChannelSubmitQueue(asrProjectInfoDataBO.getProjectNo());
        if (asrSubmitQueue == null) {
            return null;
        }
        return asrSubmitQueue.get();
    }

    private synchronized void remove(ChannelSubmitThread channelSubmitThread) {
        submitThreads.remove(channelSubmitThread);
    }


    /**
     * ASR渠道信息
     *
     * @return
     */
    public String info() {
        return "{" + asrProjectInfoDataBO.getProjectNo() + "," +
                "\"ThreadNum\":" + getChannelSubmitThreadSize() + "," +
                "\"WaitSubmit\":" + gatewayService.getGatewayServiceResource().getMonitorService().getQueueSize(asrProjectInfoDataBO.getProjectNo()) + "}";
    }

}
