package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.KeywordService;
import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import com.ytl.vos.gateway.risk.service.bo.VoiceText;
import com.ytl.vos.gateway.risk.utils.KeywordUtils;
import com.ytl.vos.persistence.dataservice.bo.RuleCustomerKeywordDataBO;
import com.ytl.vos.persistence.dataservice.bo.RuleCustomerPlanConfigDataBO;
import com.ytl.vos.persistence.dataservice.bo.RulePlatKeywordDataBO;
import com.ytl.vos.persistence.enums.KeywordConditionEnum;
import com.ytl.vos.persistence.enums.KeywordRuleModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kf-zhanghui
 * @date 2023/8/1 13:34
 */

@Service
@Slf4j
public class KeywordServiceImpl implements KeywordService {

    @Resource
    private DataService dataService;

    @Override
    public Map<String, KeyWord> keywordHandle(RuleCustomerPlanConfigDataBO planConfig, VoiceText voiceText, String taskId, String tableTime) {
        Map<String, KeyWord> keywordMap = new HashMap<>();
        StringBuilder allText = voiceText.getAllText();
        StringBuilder serviceText = voiceText.getServiceText();
        StringBuilder clientText = voiceText.getClientText();
        //先质检平台关键词
        log.warn("[Plat Keyword] {} {} ------开始进行平台关键词智检", taskId,tableTime);
        List<RulePlatKeywordDataBO> platKeywordList = dataService.getPlatKeywordList();
        if (CollUtil.isEmpty(platKeywordList)) {
            log.warn("[Plat Keyword] {} {} ------平台关键词没有配置,平台关键词智检结束",taskId,tableTime);
        } else {
            //循环遍历平台关键字
            for (RulePlatKeywordDataBO rulePlatKeywordDataBO : platKeywordList) {
                KeyWord keyWord = corePlatKeywordHandle(rulePlatKeywordDataBO, allText, serviceText, clientText);
                if(!CollectionUtils.isEmpty(keyWord.getSpiltWords()) || !CollectionUtils.isEmpty(keyWord.getSynonymWords())){
                    log.warn("[Plat Keyword] {} {} 匹配到平台关键词：{}",taskId,tableTime,keyWord.getName());
                    keywordMap.put(rulePlatKeywordDataBO.getKeyword(),keyWord);
                    //如果关键字满足条件为满足其中一条则当存在一条时，结束质检关键词
                    if(KeywordConditionEnum.PART.getCodeId() ==planConfig.getKeywordCondition()){
                        log.warn("[Plat Keyword] {} {} 质检方案:{}为满足其中一个，匹配到关键词：{},平台关键词智检结束",taskId,tableTime,planConfig.getPlanId(),keyWord.getName());
                        return keywordMap;
                    }
                }
            }
            log.warn("[Plat Keyword] {} {} ------平台关键词智检结束",taskId,tableTime);
        }
        //再质检客户关键词
        log.warn("[Customer Keyword] {} {} ------开始进行客户关键词智检",taskId,tableTime);
        String userNo = planConfig.getUserNo();
        List<RuleCustomerKeywordDataBO> custKeywordList = dataService.getCustKeywordList(userNo);
        if (CollUtil.isEmpty(custKeywordList)) {
            log.warn("[Customer Keyword] {} {} ------客户关键词没有配置,忽略客户关键词智检",taskId,tableTime);
        } else {
            //循环遍历平台关键字
            for (RuleCustomerKeywordDataBO customerKeywordDataBO : custKeywordList) {
                KeyWord keyWord = coreCustomerKeywordHandle(customerKeywordDataBO, allText, serviceText, clientText);
                if (!CollectionUtils.isEmpty(keyWord.getSpiltWords()) || !CollectionUtils.isEmpty(keyWord.getSynonymWords())) {
                    log.warn("[Customer Keyword] {} {} 匹配到客户关键词：{}",taskId,tableTime,keyWord.getName());
                    keywordMap.put(customerKeywordDataBO.getKeyword(), keyWord);
                    //如果关键字满足条件为满足其中一条则当存在一条时，结束质检关键词
                    if (KeywordConditionEnum.PART.getCodeId() == planConfig.getKeywordCondition()) {
                        log.warn("[Customer Keyword] {} {} 质检方案:{}为满足其中一个，匹配到关键词：{} ,退出关键词智能质检", taskId,tableTime,planConfig.getPlanId(),keyWord.getName());
                        return keywordMap;
                    }
                }
            }
            log.warn("[Customer Keyword] {} {} ------客户关键词智检结束",taskId,tableTime);
        }
        return keywordMap;
    }


    /**
     * 平台关键词质检
     * @param keywordDataBO 关键词对象
     * @param allText 全本
     * @param serviceText 坐席文本
     * @param clientText 客户文本
     * @return
     */
    private KeyWord corePlatKeywordHandle(RulePlatKeywordDataBO keywordDataBO, StringBuilder allText, StringBuilder serviceText, StringBuilder clientText){
        KeyWord keyWord = new KeyWord();
        keyWord.setName(keywordDataBO.getKeyword());
        keyWord.setTag(keywordDataBO.getRiskLabel());
        keyWord.setSource(SysConstant.NLP_SOURCE_PLATFORM);
        keyWord.setRuleId(keywordDataBO.getRuleId());
        AtomicInteger countTimes = new AtomicInteger(0);
        //检查范围
        Byte checkRange = keywordDataBO.getCheckRange();
        //检查模式
        Byte ruleMode = keywordDataBO.getRuleMode();
        //待检测的文本
        String waitText = KeywordUtils.getMatchText(checkRange,allText,serviceText,clientText);
        //待质检的关键词
        String[] keywordList = keywordDataBO.getKeywordSplit().split(",");
        if(KeywordRuleModeEnum.PART.getCodeId() == ruleMode){
            //部分匹配
            Map<String, Integer> part = KeywordUtils.perfectMatchKeywords(waitText, keywordList);
            if(!CollectionUtils.isEmpty(part)){
                keyWord.setSpiltWords(KeywordUtils.convertMapToList(part,keywordDataBO.getRuleValue(),countTimes));
                keyWord.setScore(keywordDataBO.getRuleValue() * countTimes.get());
            }
        }else if(KeywordRuleModeEnum.SIMILAR.getCodeId() == ruleMode) {
            //近似匹配
            Map<String, Map<String, Integer>> similar = KeywordUtils.synonymKeywords(waitText, keywordList);
            if(!CollectionUtils.isEmpty(similar)) {
                keyWord.setSynonymWords(KeywordUtils.convertMap(similar,keywordDataBO.getRuleValue(),countTimes));
                keyWord.setScore(keywordDataBO.getRuleValue() * countTimes.get());
            }
        }
        return keyWord;
    }



    /**
     * 客户关键词质检
     * @param keywordDataBO 关键词对象
     * @param allText 全本
     * @param serviceText 坐席文本
     * @param clientText 客户文本
     * @return
     */
    private KeyWord coreCustomerKeywordHandle(RuleCustomerKeywordDataBO keywordDataBO, StringBuilder allText, StringBuilder serviceText, StringBuilder clientText){
        KeyWord keyWord = new KeyWord();
        keyWord.setName(keywordDataBO.getKeyword());
        keyWord.setTag(keywordDataBO.getRiskLabel());
        keyWord.setSource(SysConstant.NLP_SOURCE_CUSTOMER);
        keyWord.setRuleId(keywordDataBO.getRuleId());
        AtomicInteger countTimes = new AtomicInteger(0);
        //检查范围
        Byte checkRange = keywordDataBO.getCheckRange();
        //检查模式
        Byte ruleMode = keywordDataBO.getRuleMode();
        //待检测的文本
        String waitText = KeywordUtils.getMatchText(checkRange,allText,serviceText,clientText);
        //待质检的关键词
        String[] keywordList = keywordDataBO.getKeywordSplit().split(",");
        if(KeywordRuleModeEnum.PART.getCodeId() == ruleMode){
            //部分匹配
            Map<String, Integer> part = KeywordUtils.perfectMatchKeywords(waitText, keywordList);
            if(!CollectionUtils.isEmpty(part)){
                keyWord.setSpiltWords(KeywordUtils.convertMapToList(part,keywordDataBO.getRuleValue(),countTimes));
                keyWord.setScore(keywordDataBO.getRuleValue() * countTimes.get());
            }
        }else if(KeywordRuleModeEnum.SIMILAR.getCodeId() == ruleMode) {
            //近似匹配
            Map<String, Map<String, Integer>> similar = KeywordUtils.synonymKeywords(waitText, keywordList);
            if(!CollectionUtils.isEmpty(similar)) {
                keyWord.setSynonymWords(KeywordUtils.convertMap(similar,keywordDataBO.getRuleValue(),countTimes));
                keyWord.setScore(keywordDataBO.getRuleValue() * countTimes.get());
            }
        }
        return keyWord;
    }








}
