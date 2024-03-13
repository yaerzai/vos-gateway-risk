package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.vos.gateway.risk.constant.RedisKeysConstant;
import com.ytl.vos.gateway.risk.constant.SysConstant;
import com.ytl.vos.gateway.risk.enums.AsrRuleEnum;
import com.ytl.vos.gateway.risk.service.*;
import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import com.ytl.vos.gateway.risk.service.bo.VoiceText;
import com.ytl.vos.gateway.risk.vo.AsrProcessResultVO;
import com.ytl.vos.gateway.risk.vo.WordFrequencyVO;
import com.ytl.vos.persistence.dataservice.CallRiskProcessDataService;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;
import com.ytl.vos.persistence.dataservice.bo.RuleCustomerPlanConfigDataBO;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author kf-zhanghui
 * @date 2023/8/1 10:10
 */

@Service
@Slf4j
public class NlpServiceImpl implements NlpService {

    @Resource
    private CallRiskProcessDataService callRiskProcessDataService;
    @Resource
    private DataService dataService;
    @Resource
    private KeywordService keywordService;
    @Resource
    private VerbalTrickService verbalTrickService;
    @Resource
    private AsrDataAnalysisService asrDataAnalysisService;
    @Resource
    private RedisCacheService redisCacheService;

    @Override
    public void nlpAnalyze(String taskId,JSONArray jsonArray) {
        long timeStamp = System.currentTimeMillis();
        if(jsonArray.size() <= 0) {
            //如果录音文件没有文本，则不做质检
            log.error("[NLP Analyze] {} ASR识别后的录音文本内容为空,{}",taskId,jsonArray.size());
            return;
        }
        //通话文本处理
        VoiceText voiceText = buildText(jsonArray);
        log.info("[NLP Analyze] {} ASR识别后的全本内容：{}",taskId,voiceText.getAllText().toString());
        log.info("[NLP Analyze] {} ASR识别后的坐席通话内容：{}",taskId,voiceText.getServiceText().toString());
        log.info("[NLP Analyze] {} ASR识别后的客户通话内容：{}",taskId,voiceText.getClientText().toString());
        //通过taskId,查询出质检信息
        CallRiskProcessDataBO callRiskProcessDataBO;
        AtomicReference<String> tableTimeRef = new AtomicReference<>();
        String tableTime;
        String manualAsrTableTime = redisCacheService.getHash(RedisKeysConstant.MANUAL_ASR_TABLE_TIME, taskId, String.class);
        if(StrUtil.isEmpty(manualAsrTableTime)){
            //自动ASR
            callRiskProcessDataBO = getCallRiskProcess(taskId, tableTimeRef);
            tableTime = tableTimeRef.get();
        }else {
            //手动ASR
            callRiskProcessDataBO =callRiskProcessDataService.getByTaskId(taskId,manualAsrTableTime);
            tableTime = manualAsrTableTime;
            log.warn("[NLP Analyze] {} {} 手动ASR智检",taskId,tableTime);
        }
//        AtomicReference<String> tableTimeRef = new AtomicReference<>();
//        CallRiskProcessDataBO callRiskProcessDataBO = getCallRiskProcess(taskId, tableTimeRef);
//        String tableTime = tableTimeRef.get();
        if(callRiskProcessDataBO == null) {
            log.error("[NLP Analyze] {} {} 未查询到ASR智检对象",taskId,tableTime);
            return;
        }
        String planId = callRiskProcessDataBO.getPlanId();
        //方案ID如果为空,则不做质检
        if(StrUtil.isEmpty(planId)){
            log.error("[NLP Analyze] {} {} ASR质检对象的质检方案为空",taskId,tableTime);
            return;
        }
        RuleCustomerPlanConfigDataBO ruleCustomerPlanConfigDataBO = dataService.getPlanConfig(planId);
        //如果客户质检配置不存在，则不做质检
        if(ruleCustomerPlanConfigDataBO == null) {
            log.error("[NLP Analyze] {} {} ASR客户质检方案不存在,planId:{}",taskId,tableTime,planId);
            return;
        }
        log.warn("[NLP Analyze] {} {} ------开始进行智能质检",taskId,tableTime);
        //关键词质检
        CompletableFuture<Map<String, KeyWord>> f1 = CompletableFuture.supplyAsync(() -> keywordService.keywordHandle(ruleCustomerPlanConfigDataBO,voiceText,taskId,tableTime));
        //话术质检
        CompletableFuture<Map<String, KeyWord>> f2 = CompletableFuture.supplyAsync(() -> verbalTrickService.scripHandle(ruleCustomerPlanConfigDataBO,voiceText,taskId,tableTime));
        AtomicBoolean isRunning = new AtomicBoolean(true);
        CompletableFuture.allOf(f1, f2).thenRunAsync(() -> {
            log.warn("[NLP Analyze] {} {} 关键词和话术并行完成,开始组装数据",taskId,tableTime);
            try {
                Map<String, KeyWord> keywordNlp = f1.get();
                log.info("[NLP Analyze] {} {} 原NLP质检关键词：{}", taskId,tableTime,JSONUtil.toJsonStr(keywordNlp));
                Map<String, KeyWord> scripNlp = f2.get();
                log.info("[NLP Analyze] {} {} 原NLP质检话术：{}", taskId,tableTime,JSONUtil.toJsonStr(scripNlp));
                //平台匹配集合
                Map<String, List <WordFrequencyVO>> platHitRule = new HashMap<>();
                //客户匹配集合
                Map<String, List <WordFrequencyVO>> customerHitRule = new HashMap<>();
                //总分数,默认值0
                AtomicInteger countScore = new AtomicInteger(0);
                //标签
                List<String> tags = new ArrayList<>();
                buildKeywordProcess(keywordNlp,platHitRule,customerHitRule,countScore,tags);
                buildScripProcess(scripNlp,platHitRule,customerHitRule,countScore);
                log.info("[NLP Analyze] {} {} 转化后的NLP质检平台集合：{}", taskId,tableTime,JSONUtil.toJsonStr(platHitRule));
                log.info("[NLP Analyze] {} {} 转化后的NLP质检客户集合：{}", taskId,tableTime,JSONUtil.toJsonStr(customerHitRule));
                log.info("[NLP Analyze] {} {} 转化后的NLP质检总分数、总标签集合：{}-{}",taskId,tableTime,countScore.get(),JSONUtil.toJsonStr(tags));
                AsrProcessResultVO asrProcessResultVO = new AsrProcessResultVO();
                asrProcessResultVO.setAsrProcessId(callRiskProcessDataBO.getId());
                asrProcessResultVO.setCallId(callRiskProcessDataBO.getCallId());
                asrProcessResultVO.setTaskId(taskId);
                asrProcessResultVO.setUserNo(callRiskProcessDataBO.getUserNo());
                asrProcessResultVO.setCustomerNo(callRiskProcessDataBO.getCustomerNo());
                asrProcessResultVO.setPlanId(planId);
                asrProcessResultVO.setFileUrl(callRiskProcessDataBO.getFileUrl());
                asrProcessResultVO.setTableTime(tableTime);
                asrProcessResultVO.setAnalysisFinishTime(DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS_C));
                asrProcessResultVO.setPlatHitRule(platHitRule);
                asrProcessResultVO.setCustomerHitRule(customerHitRule);
                if(CollectionUtils.isEmpty(tags)){
                    asrProcessResultVO.setTag("");
                }else {
                    asrProcessResultVO.setTag(tags.stream().collect(Collectors.joining(",")));
                }
                asrProcessResultVO.setScore(countScore.toString());
                asrDataAnalysisService.addProcessResult(asrProcessResultVO);
            }catch (Exception e) {
                log.error("[NLP Analyze]  NLP质检发生异常",e);
            }finally {
                isRunning.set(false);
            }
        });
        while (isRunning.get()) {
            ThreadUtil.sleep(100);
        }
        log.warn("[NLP Analyze] {} NLP质检完成,总耗时：{}",taskId,System.currentTimeMillis() - timeStamp);
    }

    //获取质检处理任务，当天和前一天
    private CallRiskProcessDataBO getCallRiskProcess(String taskId, AtomicReference<String> tableTimeRef) {
        String tableTime = DateUtil.date().toString(DatePattern.PURE_DATE_PATTERN);
        CallRiskProcessDataBO callRiskProcessDataBO = callRiskProcessDataService.getByTaskId(taskId, tableTime);
        if (callRiskProcessDataBO != null) {
            tableTimeRef.set(tableTime);
            return callRiskProcessDataBO;
        }
        tableTime = DateUtil.offsetDay(DateUtil.date(), -1).toString(DatePattern.PURE_DATE_PATTERN);
        callRiskProcessDataBO = callRiskProcessDataService.getByTaskId(taskId, tableTime);
        tableTimeRef.set(tableTime);
        return callRiskProcessDataBO;
    }



    /**
     * 组装文本内容
     * @param jsonArray 回调结果
     * @param allText 全本
     * @param serviceText 坐席文本
     * @param clientText 客户文本
     */
    private VoiceText buildText(JSONArray jsonArray){

        VoiceText voiceText = new VoiceText();
        StringBuilder allText = new StringBuilder();
        StringBuilder serviceText = new StringBuilder();
        StringBuilder clientText = new StringBuilder();
        //默认第一个说话的就是坐席；
        String serviceId = jsonArray.getJSONObject(0).getString("ChannelId");
        //组装文本
        for (int i = 0; i < jsonArray.size(); i++) {
            String channelId = jsonArray.getJSONObject(i).getString("ChannelId");
            String text = jsonArray.getJSONObject(i).getString("Text");
            if(StrUtil.isEmpty(text)) {
                continue;
            }
            if(serviceId.equals(channelId)){
                serviceText.append(text).append("|");
            }else {
                clientText.append(text).append("|");
            }
            allText.append(text).append("|");
        }
        if(allText.toString().endsWith("|")){
            allText.deleteCharAt(allText.length() -1);
        }
        if(serviceText.toString().endsWith("|")){
            serviceText.deleteCharAt(serviceText.length() -1);
        }
        if(clientText.toString().endsWith("|")){
            clientText.deleteCharAt(clientText.length() -1);
        }
        voiceText.setAllText(allText);
        voiceText.setServiceText(serviceText);
        voiceText.setClientText(clientText);
        return voiceText;
    }


    /**
     *组装数据
     * @param keywords
     * @param platHitRule
     * @param customerHitRule
     * @param countScore
     * @param countTag
     */
    private void buildKeywordProcess(Map<String, KeyWord> keywords,Map<String, List <WordFrequencyVO>> platHitRule,
                                     Map<String, List <WordFrequencyVO>> customerHitRule,AtomicInteger countScore,List<String> countTag){
        if(!platHitRule.containsKey(AsrRuleEnum.KEY_WORD.getCode())){
            platHitRule.put(AsrRuleEnum.KEY_WORD.getCode(),new ArrayList<>());
        }
        if(!customerHitRule.containsKey(AsrRuleEnum.KEY_WORD.getCode())){
            customerHitRule.put(AsrRuleEnum.KEY_WORD.getCode(),new ArrayList<>());
        }
        if(!CollectionUtils.isEmpty(keywords)){
            for (KeyWord keyWord : keywords.values()) {
                WordFrequencyVO wordFrequencyVO = new WordFrequencyVO();
                wordFrequencyVO.setRuleId(keyWord.getRuleId());
                wordFrequencyVO.setName(keyWord.getName());
                wordFrequencyVO.setSpiltWords(keyWord.getSpiltWords());
                wordFrequencyVO.setSynonymWords(keyWord.getSynonymWords());
                if(SysConstant.NLP_SOURCE_PLATFORM.equals(keyWord.getSource())){
                    List<WordFrequencyVO> wordFrequencyVOS = platHitRule.get(AsrRuleEnum.KEY_WORD.getCode());
                    wordFrequencyVOS.add(wordFrequencyVO);
                }else {
                    List<WordFrequencyVO> wordFrequencyVOS = customerHitRule.get(AsrRuleEnum.KEY_WORD.getCode());
                    wordFrequencyVOS.add(wordFrequencyVO);
                }
                countTag.add(keyWord.getTag());
                countScore.addAndGet(keyWord.getScore());
            }
        }
    }


    /**
     *话术数据组装
     * @param scrips
     * @param platHitRule
     * @param customerHitRule
     * @param countScore
     */
    private void buildScripProcess(Map<String, KeyWord> scrips,Map<String, List <WordFrequencyVO>> platHitRule,
                                     Map<String, List <WordFrequencyVO>> customerHitRule,AtomicInteger countScore){
        if(!platHitRule.containsKey(AsrRuleEnum.VERBAL_TRICK.getCode())){
            platHitRule.put(AsrRuleEnum.VERBAL_TRICK.getCode(),new ArrayList<>());
        }
        if(!customerHitRule.containsKey(AsrRuleEnum.VERBAL_TRICK.getCode())){
            customerHitRule.put(AsrRuleEnum.VERBAL_TRICK.getCode(),new ArrayList<>());
        }
        if(!CollectionUtils.isEmpty(scrips)){
            for (KeyWord keyWord : scrips.values()) {
                WordFrequencyVO wordFrequencyVO = new WordFrequencyVO();
                wordFrequencyVO.setRuleId(keyWord.getRuleId());
                wordFrequencyVO.setName(keyWord.getName());
                wordFrequencyVO.setSpiltWords(keyWord.getSpiltWords());
                wordFrequencyVO.setSynonymWords(keyWord.getSynonymWords());
                if(SysConstant.NLP_SOURCE_PLATFORM.equals(keyWord.getSource())){
                    List<WordFrequencyVO> wordFrequencyVOS = platHitRule.get(AsrRuleEnum.VERBAL_TRICK.getCode());
                    wordFrequencyVOS.add(wordFrequencyVO);
                }else {
                    List<WordFrequencyVO> wordFrequencyVOS = customerHitRule.get(AsrRuleEnum.VERBAL_TRICK.getCode());
                    wordFrequencyVOS.add(wordFrequencyVO);
                }
                countScore.addAndGet(keyWord.getScore());
            }
        }
    }
}
