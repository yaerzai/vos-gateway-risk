package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.service.MailService;
import com.ytl.common.base.service.bo.MailSimpleSendBO;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.base.utils.SmsUtil;
import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.dto.SmsSendReqDTO;
import com.ytl.vos.gateway.risk.enums.NotifyTypeEnum;
import com.ytl.vos.gateway.risk.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.risk.jms.product.ReCheckNotifyProducer;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.WarnService;
import com.ytl.vos.gateway.risk.utils.HttpUtils;

import com.ytl.vos.gateway.risk.dto.PlatformNotifyDTO;
import com.ytl.vos.persistence.dataservice.PlatNoteMessageDataService;
import com.ytl.vos.persistence.dataservice.bo.PlatNoteConfigDataBO;
import com.ytl.vos.persistence.dataservice.bo.PlatNoteMessageDataBO;
import com.ytl.vos.persistence.enums.MonitorWarnTypeEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lingchuanyu
 * @date 2023/7/31-16:03
 */
@Service
@Slf4j
public class WarnServiceImpl implements WarnService {

    @Resource
    private MailService mailService;

    @Autowired
    DataService dataService;

    @Resource
    ReCheckNotifyProducer reCheckNotifyProducer;

    @Autowired
    private PlatNoteMessageDataService platNoteMessageDataService;

    private static final String TEST_DEMO = "1";
    private static final String SUCCESS_CODE = "0000";
    private static final String CODE = "code";
    private static final String MSG = "msg";


    @Override
    public void sendWarn(PlatNoteConfigDataBO platNoteConfigDataBO, String warnContent) {
        List<PlatNoteConfigDataBO> list = Lists.newArrayList();
        list.add(platNoteConfigDataBO);
        baseSendWarn(list, MonitorWarnTypeEnum.RE_CHECK_TASK_NOTIFY, warnContent);
    }


    /**
     * 根据配置好的预警联系人发送预警
     *
     * @param configList  需要发送的联系人
     * @param warnType    发送类型
     * @param warnContent 发送内容
     */
    private void baseSendWarn(List<PlatNoteConfigDataBO> configList, MonitorWarnTypeEnum warnType, String warnContent) {
        if (CollectionUtils.isEmpty(configList)) {
            log.warn("[WarnService] 该类型未配置预警联系人，warnType:{}", warnType.getCodeName());
            return;
        }
        configList.forEach(item -> {
            log.info("[WarnService] PlatNoteConfigDataBO:{}", item);
            String[] split = item.getNoteType().split(",");
            for (String s : split) {
                // 一个联系人可以多个通知方式,case不进行break截断
                NotifyTypeEnum notifyTypeEnum = NotifyTypeEnum.parse(s);
                switch (notifyTypeEnum) {
                    // 公众号
                    case WECHAT:
                        if (StringUtils.isNotBlank(item.getOpenid())) {
                            SysConstant.warnSendExecutors.execute(() -> sendWarnWechat(warnContent, item.getOpenid()));
                        }
                        break;
                        // 短信发送
                    case SMS:
                        if (StringUtils.isNotBlank(item.getMobileNo())) {
                            SysConstant.warnSendExecutors.execute(() -> sendWarnSms(warnContent, item.getMobileNo()));
                        }
                        break;
                        // 邮件发送
                    case EMAIL:
                        List<String> eMailList = new ArrayList<>();
                        if (StringUtils.isNotBlank(item.getEmail())) {
                            eMailList.add(item.getEmail());
                            SysConstant.warnSendExecutors.execute(() -> sendWarnEmail(warnType, warnContent, eMailList));
                        }
                        break;
                        // 默认平台发送
                    case PLATFORM:
                        if (StringUtils.isNotBlank(item.getSysUserNo())) {
                            sendWarnPlatform(warnContent, item.getSysUserNo());
                        }
                        break;

                }
            }

        });


    }


    /**
     * 邮件发送预警
     *
     * @param warnType
     * @param content
     * @param addressList
     */
    private void sendWarnEmail(MonitorWarnTypeEnum warnType, String content, List<String> addressList) {
        content = content.replaceAll("【" + StringUtils.defaultIfBlank(SmsUtil.getSign(content), "") + "】", "");
        MailSimpleSendBO simpleSendBO = MailSimpleSendBO.builder().subject(warnType.getCodeName()).content(content).build();
        simpleSendBO.setReceiver(addressList);
        mailService.sendSimpleMail(simpleSendBO);
    }

    /**
     * 短信发送预警
     *
     * @param warnContent
     * @param mobileNo
     */
    private void sendWarnSms(String warnContent, String mobileNo) {
        // 准备发送短信参数
        SmsSendReqDTO sendSmsRequest = new SmsSendReqDTO();
        // 先使用该账号进行预警通知，后续可以通过系统参数配置
        String cidAndPrivateKey = dataService.getSysParam(SysParamEnum.SMS_MOBILE_VERIFICATION_CID_PRIVATEKEY);
        String[] split = cidAndPrivateKey.split("\\|");
        if (2 != split.length) {
            throw new BusinessException(VosErrCodeEnum.SMS_SECRET_ERROR);
        }
        String accessKey = split[0];
        String accessKeySecret = split[1];
        //短信文本内容
        sendSmsRequest.setContent(warnContent);
        sendSmsRequest.setCid(accessKey);
        sendSmsRequest.setMobileNo(mobileNo);
        String timeStamp = DateUtil.date().toString(DatePattern.PURE_DATETIME_PATTERN);
        sendSmsRequest.setTimeStamp(timeStamp);
        sendSmsRequest.setSign(SecureUtil.md5(accessKey + accessKeySecret + timeStamp));
        // 如果是测试模式直接日志打印输出
        if (TEST_DEMO.equals(dataService.getSysParam(SysParamEnum.SMS_MOBILE_VERIFICATION_TEMPLATE))) {
            log.info("测试模式 - 接收手机：{},内容：{}", mobileNo, warnContent);
        } else {
            log.info("接收手机：{},内容：{}", mobileNo, warnContent);
            //发送短信
            this.sendSmsCode(sendSmsRequest);
        }

    }

    /**
     * 短信发送
     *
     * @param smsSendReqDTO
     */
    public void sendSmsCode(SmsSendReqDTO smsSendReqDTO) {
        //从系统参数获取短信平台的发送短信接口URL;
        String smsUrl = dataService.getSysParam(SysParamEnum.SMS_REQUEST_URL);
        String reqBody = JSONUtil.toJsonStr(smsSendReqDTO);
        try {
            String respBody = HttpUtils.postJson(smsUrl, reqBody);
            if (StrUtil.isEmpty(respBody)) {
                throw new BusinessException(VosErrCodeEnum.SMS_SERVER_REQUEST_ERROR.getMsg());
            }
            JSONObject respJson = JSONUtil.parseObj(respBody);
            String code = respJson.getStr(CODE);
            String msg = respJson.getStr(MSG);
            if (!SUCCESS_CODE.equalsIgnoreCase(code)) {
                log.error("短信请求失败, code: {}, msg: {}", code, msg);
                throw new BusinessException(StrUtil.format("短信请求失败,{}", msg));
            }
        } catch (Exception e) {
            log.error("发送短信错误", e);
            throw new BusinessException(VosErrCodeEnum.SMS_SERVER_REQUEST_ERROR.getMsg());
        }
    }

    /**
     * 站内消息发送预警
     *
     * @param warnContent
     * @param userNo
     */
    private void sendWarnPlatform(String warnContent, String userNo) {

        // 消息推送入库
        PlatNoteMessageDataBO platNoteMessageDataBO = PlatNoteMessageDataBO.builder()
                .noteTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_E))
                .sysUserNo(userNo)
                .noteMessage(warnContent)
                .noteType((byte) 3)
                .build();
        Long msgId = platNoteMessageDataService.add(platNoteMessageDataBO);
        if (-1 == msgId) {
            log.error("消息入库失败,内容:{},userNo:{}",warnContent,userNo);
            return;
        }
        // mq推送到managerAPI中,webSocket推送
        PlatformNotifyDTO platformNotifyVO = PlatformNotifyDTO.builder()
                .msgId(msgId).userNo(userNo).warnContent(warnContent).build();
        reCheckNotifyProducer.send(platformNotifyVO);
    }

    // todo 发送微信公众号预警
    private void sendWarnWechat(String warnContent, String openId) {

    }


}
