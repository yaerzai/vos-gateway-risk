package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.VerbalTrickService;
import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import com.ytl.vos.gateway.risk.service.bo.VoiceText;
import com.ytl.vos.gateway.risk.utils.KeywordUtils;
import com.ytl.vos.persistence.dataservice.bo.*;
import com.ytl.vos.persistence.enums.KeywordConditionEnum;
import com.ytl.vos.persistence.enums.KeywordRuleModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kf-zhanghui
 * @date 2023/8/1 17:05
 */

@Service
@Slf4j
public class VerbalTrickServiceImpl implements VerbalTrickService {

    @Resource
    private DataService dataService;

    @Override
    public Map<String, KeyWord> scripHandle(RuleCustomerPlanConfigDataBO planConfig,VoiceText voiceText, String taskId, String tableTime) {

        Map<String, KeyWord> keywordMap = new HashMap<>();
        StringBuilder allText = voiceText.getAllText();
        StringBuilder serviceText = voiceText.getServiceText();
        StringBuilder clientText = voiceText.getClientText();
        //先匹配平台话术
        log.warn("[PLat VerbalTrick] {} {} ----------开始进行平台话术智能匹配", taskId,tableTime);
        List<RulePlatScripDataBO> platScripList = dataService.getPlatScripList();
        if (CollUtil.isEmpty(platScripList)) {
            log.warn("[PLat VerbalTrick] {} {} ------平台话术没有配置,平台话术智能匹配结束",taskId,tableTime);
        } else {
            //循环遍历平台话术
            for (RulePlatScripDataBO rulePlatScripDataBO : platScripList) {
                KeyWord keyWord = corePlatScripHandle(rulePlatScripDataBO, allText, serviceText, clientText);
                if(!CollectionUtils.isEmpty(keyWord.getSpiltWords()) || !CollectionUtils.isEmpty(keyWord.getSynonymWords())){
                    log.warn("[PLat VerbalTrick] {} {} 匹配到平台话术：{}",taskId,tableTime,keyWord.getName());
                    keywordMap.put(rulePlatScripDataBO.getKeyword(),keyWord);
                    //如果关键字满足条件为满足其中一条则当存在一条时，结束质检关键词
                    if(KeywordConditionEnum.PART.getCodeId() ==planConfig.getScripCondition()){
                        log.warn("[PLat VerbalTrick] {} {} 质检方案:{}为满足其中一个，匹配到平台话术：{},平台话术智能匹配结束",taskId,tableTime,planConfig.getPlanId(),keyWord.getName());
                        return keywordMap;
                    }
                }
            }
            log.warn("[PLat VerbalTrick] {} {} ------平台话术智能匹配结束",taskId,tableTime);
        }
        //再匹配客户话术
        log.warn("[Customer VerbalTrick] {} {} ------开始进行客户话术智能匹配",taskId,tableTime);
        String userNo = planConfig.getUserNo();
        List<RuleCustomerScripDataBO> custScripList = dataService.getCustScripList(userNo);

        if (CollUtil.isEmpty(custScripList)) {
            log.warn("[Customer VerbalTrick] {} {} ------客户话术没有配置,客户话术智能匹配结束",taskId,tableTime);
        } else {
            //循环遍历客户话术
            for (RuleCustomerScripDataBO customerScripDataBO : custScripList) {
                KeyWord keyWord = coreCustomerScripHandle(customerScripDataBO, allText, serviceText, clientText);
                if(!CollectionUtils.isEmpty(keyWord.getSpiltWords()) || !CollectionUtils.isEmpty(keyWord.getSynonymWords())){
                    log.warn("[Customer VerbalTrick] {} {} 匹配到客户话术：{}",taskId,tableTime,keyWord.getName());
                    keywordMap.put(customerScripDataBO.getKeyword(),keyWord);
                    //如果关键字满足条件为满足其中一条则当存在一条时，结束质检关键词
                    if(KeywordConditionEnum.PART.getCodeId() ==planConfig.getScripCondition()){
                        log.warn("[Customer VerbalTrick] {} {} 质检方案：{}为满足其中一个，匹配到客户话术：{},客户话术智能匹配结束",taskId,tableTime,planConfig.getPlanId(),keyWord.getName());
                        return keywordMap;
                    }
                }
            }
            log.warn("[Customer VerbalTrick] {} {} ------客户话术智能匹配结束",taskId,tableTime);
        }
        return keywordMap;
    }

    /**
     * 平台话术质检
     * @param scripDataBO 话术对象
     * @param allText 全本
     * @param serviceText 坐席文本
     * @param clientText 客户文本
     * @return
     */
    private KeyWord corePlatScripHandle(RulePlatScripDataBO scripDataBO, StringBuilder allText, StringBuilder serviceText, StringBuilder clientText){
        KeyWord keyWord = new KeyWord();
        keyWord.setName(scripDataBO.getKeyword());
        keyWord.setTag(scripDataBO.getRuleDesc());
        keyWord.setSource(SysConstant.NLP_SOURCE_PLATFORM);
        keyWord.setRuleId(scripDataBO.getRuleId());
        AtomicInteger countTimes = new AtomicInteger(0);
        //检查范围
        Byte checkRange = scripDataBO.getCheckRange();
        //检查模式
        Byte ruleMode = scripDataBO.getRuleMode();
        //待检测的文本
        String waitText = KeywordUtils.getMatchText(checkRange,allText,serviceText,clientText);
        //待质检的关键词
        if(KeywordRuleModeEnum.PART.getCodeId() == ruleMode){
            //部分匹配
            Map<String, Integer> part = KeywordUtils.perfectMatchKeywords(waitText, Arrays.copyOf(new String[]{scripDataBO.getKeyword()},1));
            if(!CollectionUtils.isEmpty(part)){
                keyWord.setSpiltWords(KeywordUtils.convertMapToList(part,scripDataBO.getRuleValue(),countTimes));
                keyWord.setScore(scripDataBO.getRuleValue() * countTimes.get());
            }
        }else if(KeywordRuleModeEnum.SIMILAR.getCodeId() == ruleMode) {
            //近似匹配
            log.info("[PLat VerbalTrick] 开始进行话术近似匹配：{}",scripDataBO.getKeyword());
            Map<String, Map<String, Integer>> similar = KeywordUtils.synonymKeywords(waitText, Arrays.copyOf(new String[]{scripDataBO.getKeyword()},1));
            if(!CollectionUtils.isEmpty(similar)) {
                keyWord.setSynonymWords(KeywordUtils.convertMap(similar,scripDataBO.getRuleValue(),countTimes));
                keyWord.setScore(scripDataBO.getRuleValue() * countTimes.get());
            }
        }
        return keyWord;
    }



    /**
     * 客户话术质检
     * @param customerScripDataBO 客户话术对象
     * @param allText 全本
     * @param serviceText 坐席文本
     * @param clientText 客户文本
     * @return
     */
    private KeyWord coreCustomerScripHandle(RuleCustomerScripDataBO customerScripDataBO, StringBuilder allText, StringBuilder serviceText, StringBuilder clientText){
        KeyWord keyWord = new KeyWord();
        keyWord.setName(customerScripDataBO.getKeyword());
        keyWord.setTag(customerScripDataBO.getRuleDesc());
        keyWord.setSource(SysConstant.NLP_SOURCE_CUSTOMER);
        keyWord.setRuleId(customerScripDataBO.getRuleId());
        AtomicInteger countTimes = new AtomicInteger(0);
        //检查范围
        Byte checkRange = customerScripDataBO.getCheckRange();
        //检查模式
        Byte ruleMode = customerScripDataBO.getRuleMode();
        //待检测的文本
        String waitText = KeywordUtils.getMatchText(checkRange,allText,serviceText,clientText);
        if(KeywordRuleModeEnum.PART.getCodeId() == ruleMode){
            //部分匹配
            Map<String, Integer> part = KeywordUtils.perfectMatchKeywords(waitText, Arrays.copyOf(new String[]{customerScripDataBO.getKeyword()},1));
            if(!CollectionUtils.isEmpty(part)){
                keyWord.setSpiltWords(KeywordUtils.convertMapToList(part,customerScripDataBO.getRuleValue(),countTimes));
                keyWord.setScore(customerScripDataBO.getRuleValue() * countTimes.get());
            }
        }else if(KeywordRuleModeEnum.SIMILAR.getCodeId() == ruleMode) {
            //近似匹配
            Map<String, Map<String, Integer>> similar = KeywordUtils.synonymKeywords(waitText, Arrays.copyOf(new String[]{customerScripDataBO.getKeyword()},1));
            if(!CollectionUtils.isEmpty(similar)) {
                keyWord.setSynonymWords(KeywordUtils.convertMap(similar,customerScripDataBO.getRuleValue(),countTimes));
                keyWord.setScore(customerScripDataBO.getRuleValue() * countTimes.get());
            }
        }
        return keyWord;
    }

}
