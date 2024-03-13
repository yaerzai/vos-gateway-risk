package com.ytl.vos.gateway.risk.service;

import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import com.ytl.vos.gateway.risk.service.bo.VoiceText;
import com.ytl.vos.persistence.dataservice.bo.RuleCustomerPlanConfigDataBO;

import java.util.Map;

/**
 * @author kf-zhanghui
 * @date 2023/8/1 17:04
 * 话术服务
 */
public interface VerbalTrickService {


    /**
     * 话术质检
     * @param planConfigData 质检方案
     * @param voiceText 文本对象
     * @param taskId 任务ID
     * @param tableTime 分表时间
     * @return
     */
    Map<String, KeyWord> scripHandle(RuleCustomerPlanConfigDataBO planConfigData,VoiceText voiceText, String taskId, String tableTime);
}
