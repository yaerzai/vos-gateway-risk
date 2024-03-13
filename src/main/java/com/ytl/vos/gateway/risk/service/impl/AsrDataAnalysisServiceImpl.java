package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoUpdateDTO;
import com.ytl.vos.customer.api.service.CustomerUserInfoService;
import com.ytl.vos.gateway.risk.enums.AsrRuleEnum;
import com.ytl.vos.gateway.risk.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.risk.service.AsrDataAnalysisService;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.WarnService;
import com.ytl.vos.gateway.risk.vo.AsrProcessResultVO;
import com.ytl.vos.gateway.risk.vo.WordFrequencyVO;
import com.ytl.vos.persistence.dataservice.*;
import com.ytl.vos.persistence.dataservice.bo.*;
import com.ytl.vos.persistence.enums.RiskNlpResultEnum;
import com.ytl.vos.persistence.enums.StatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lingchuanyu
 * @date 2023/7/25-16:15
 */
@Slf4j
@Service
public class AsrDataAnalysisServiceImpl implements AsrDataAnalysisService {

    @Autowired
    RuleCustomerPlanConfigDataService ruleCustomerPlanConfigDataService;

    @Autowired
    private CallRiskCheckDataService callRiskCheckDataService;

    @Autowired
    CallLogDataService callLogDataService;

    @Autowired
    PlatCallBlackMobileDataService platCallBlackMobileDataService;

    @Autowired
    CallRiskProcessDataService callRiskProcessDataService;

    @Autowired
    private PlatNoteConfigDataService platNoteConfigDataService;

    @Autowired
    DataService dataService;

    @Autowired
    private WarnService warnService;

    @Autowired
    private CustomerUserInfoService customerUserInfoService;

    private static final String STOP = "STOP";
    private static final String RE_CHECK = "RE_CHECK";
    private static final String CONTENT_TEMP = "您有一条语音质检需要进行人工复核,该复核流程由系统分配,话单id为:%s";

    @Override
    public void addProcessResult(AsrProcessResultVO asrProcessResultVO) {
        log.info("[AsrDataAnalysis] {} {} 请求参数：{}",asrProcessResultVO.getTaskId(),asrProcessResultVO.getTableTime(),JSONUtil.toJsonStr(asrProcessResultVO));
        CallRiskProcessDataBO callRiskProcessDataBO = new CallRiskProcessDataBO();
        // 预先结果为正常
        callRiskProcessDataBO.setNlpResult(RiskNlpResultEnum.AUTO_PASS.getCodeId());
        // 初始化构建动作
        Map<String, Boolean> resultTypeMap = Maps.newHashMap();
        resultTypeMap.put(STOP, false);
        resultTypeMap.put(RE_CHECK, false);
        // 获取方案id和语音文件地址
        String planId = asrProcessResultVO.getPlanId();
        String fileUrl = asrProcessResultVO.getFileUrl();
        // 获取制定的相应规则方案信息
        // 使用缓存获取
        RuleCustomerPlanConfigDataBO ruleCustomerPlanConfigDataBO = dataService.getPlanConfig(planId);
        if (ruleCustomerPlanConfigDataBO == null) {
            log.error("[AsrDataAnalysis] {} {} planId:{} 客户质检方案为空",asrProcessResultVO.getTaskId(),asrProcessResultVO.getTableTime(),planId);
            throw new BusinessException("客户的质检方案为空，PlanId:" + planId);
        }

        BigDecimal score = BigDecimal.valueOf(Double.parseDouble(asrProcessResultVO.getScore()));
        String passScoreRangeStr = ruleCustomerPlanConfigDataBO.getPassScoreRange();
        BigDecimal passScoreRange = BigDecimal.valueOf(Double.parseDouble(passScoreRangeStr));
        // 如果有制定规则,则进行分数比较,当比目标值小时,不合规则进行人工复核
        if (score.compareTo(passScoreRange) < 0) {
            resultTypeMap.put(RE_CHECK, true);
        }

        // 方案里面设置有随机或者指定复核人员才能进行消息通知
        String taskOperator = ruleCustomerPlanConfigDataBO.getTaskOpertor();
        PlatNoteConfigDataBO sysTaskOpertor = null;
        if (StringUtils.isNotBlank(taskOperator)) {
            // 随机,否则是指定。
            if ("1".equals(taskOperator)) {
                // 加缓存
                List<PlatNoteConfigDataBO> platNoteCfgList = dataService.getPlatNoteCfgList();
                if (CollectionUtils.isNotEmpty(platNoteCfgList)) {
                    sysTaskOpertor = RandomUtil.randomEle(platNoteCfgList);
                }
            } else {
                // 加缓存
                sysTaskOpertor = dataService.getPlatNoteCfg(taskOperator);
            }
        }
        Map<String, List<WordFrequencyVO>> platHitRule = asrProcessResultVO.getPlatHitRule();
        Map<String, List<WordFrequencyVO>> customerHitRule = asrProcessResultVO.getCustomerHitRule();
        // 进行平台规则命中解析
        if (MapUtils.isNotEmpty(platHitRule)) {
            platKeyWordHandle(platHitRule.get(AsrRuleEnum.KEY_WORD.getCode()), resultTypeMap,asrProcessResultVO.getTaskId());
        }
        // 进行客户规则命中解析
        if (MapUtils.isNotEmpty(customerHitRule)) {
            customerKeyWordHandle(customerHitRule.get(AsrRuleEnum.KEY_WORD.getCode()), resultTypeMap,asrProcessResultVO.getTaskId());
        }
        // 判断关停还是只需要复核
        if (resultTypeMap.get(STOP)) {
            // 2、进行进行复核流程创建，将匹配的词加入到复核流程中
            addReCheckTask(asrProcessResultVO, planId, fileUrl, sysTaskOpertor);
            callRiskProcessDataBO.setNlpResult(RiskNlpResultEnum.WAIT_CHECK.getCodeId());
            // 3、添加复核预警信息
            warnService.sendWarn(sysTaskOpertor, String.format(dataService.getSysParam(SysParamEnum.ASR_SMS_MOBILE_WARN_TEMPLATE), asrProcessResultVO.getCallId()));
            // 4、检查规则是否关停
            if (ruleCustomerPlanConfigDataBO.getStopFlag() == 1) {
                // 1、根据话单id，获取主叫号码加入黑名单
                CallLogDataBO callLogDataBO = callLogDataService.getByCallId(asrProcessResultVO.getCallId(), asrProcessResultVO.getTableTime());
                if (callLogDataBO == null) {
                    log.error("[AsrDataAnalysis] {} {} 未找到相应的话单记录callId:{}",asrProcessResultVO.getTaskId(),asrProcessResultVO.getTableTime(),asrProcessResultVO.getCallId());
                    throw new BusinessException(VosErrCodeEnum.CALL_LOG_BLANK.getMsg());
                }
                PlatCallBlackMobileDataBO addBo = PlatCallBlackMobileDataBO.builder().mobileNo(callLogDataBO.getCallingNumber()).build();
                platCallBlackMobileDataService.add(addBo);

                CustomerUserInfoUpdateDTO updateDTO = CustomerUserInfoUpdateDTO.builder().build();
                updateDTO.setUserNo(asrProcessResultVO.getUserNo());
                updateDTO.setStatus(StatusEnum.DISABLE.getCodeId());
                customerUserInfoService.update(updateDTO);
                // 关停账号处理
                callRiskProcessDataBO.setNlpResult(RiskNlpResultEnum.AUTO_REJECT.getCodeId());
            }

        } else if (resultTypeMap.get(RE_CHECK)) {
            // 未监测到关键词拦截,但是匹配到人工复核,进行流程创建，将匹配的词加入到复核流程中
            addReCheckTask(asrProcessResultVO, planId, fileUrl, sysTaskOpertor);
            callRiskProcessDataBO.setNlpResult(RiskNlpResultEnum.WAIT_CHECK.getCodeId());
            // 添加预警信息
            warnService.sendWarn(sysTaskOpertor, String.format(dataService.getSysParam(SysParamEnum.ASR_SMS_MOBILE_WARN_TEMPLATE), asrProcessResultVO.getCallId()));
        }
        // 将npl处理数据入库更新
        callRiskProcessDataBO.setHitSysRules(JSONObject.toJSON(asrProcessResultVO.getPlatHitRule()).toString());
        callRiskProcessDataBO.setHitUserRules(JSONObject.toJSON(asrProcessResultVO.getCustomerHitRule()).toString());
        callRiskProcessDataBO.setNlpDesc(asrProcessResultVO.getNlpDesc());
        callRiskProcessDataBO.setNlpScore(asrProcessResultVO.getScore());
        callRiskProcessDataBO.setNlpLabel(asrProcessResultVO.getTag());
        callRiskProcessDataBO.setNlpDoneTime(asrProcessResultVO.getAnalysisFinishTime());
        callRiskProcessDataBO.setId(asrProcessResultVO.getAsrProcessId());
        callRiskProcessDataBO.setTableTime(asrProcessResultVO.getTableTime());
        callRiskProcessDataService.update(callRiskProcessDataBO);
    }

    /**
     * 创建复核任务表
     *
     * @param asrProcessResultVO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = {Exception.class})
    public void addReCheckTask(AsrProcessResultVO asrProcessResultVO, String planId, String voideURL, PlatNoteConfigDataBO sysTaskOpertor) {
        CallRiskCheckDataBO callRiskCheckDataBO = CallRiskCheckDataBO.builder()
                .callId(asrProcessResultVO.getCallId())
                .taskId(asrProcessResultVO.getTaskId())
                .userNo(asrProcessResultVO.getUserNo())
                .customerNo(asrProcessResultVO.getCustomerNo())
                .planId(planId)
                .fileUrl(voideURL)
                .nlpDesc(asrProcessResultVO.getNlpDesc())
                .nlpLabel(asrProcessResultVO.getTag())
                .nlpScore(asrProcessResultVO.getScore())
                .nlpDoneTime(asrProcessResultVO.getAnalysisFinishTime())
                .checkSysOperator(sysTaskOpertor.getPlatUserNo())
                .tableTime(asrProcessResultVO.getTableTime())
                .build();
        if (sysTaskOpertor != null) {
            callRiskCheckDataBO.setCheckSysOperator(sysTaskOpertor.getPlatUserNo());
        }
        callRiskCheckDataService.add(callRiskCheckDataBO);
    }

    /**
     * 平台关键擦监测
     *
     * @param wordFrequencyVOList
     * @param resultTypeMap
     */
    private void platKeyWordHandle(List<WordFrequencyVO> wordFrequencyVOList, Map<String, Boolean> resultTypeMap,String taskId) {
        List<String> ruleIdList = wordFrequencyVOList.stream().map(WordFrequencyVO::getRuleId).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ruleIdList)) {
            for (String ruleId : ruleIdList) {
                // 加redis缓存
                RulePlatKeywordDataBO rulePlatKeyword = dataService.getPlatKeyword(ruleId);
                if (rulePlatKeyword == null || rulePlatKeyword.getRuleAction() == null) {
                    continue;
                }
                // 关键词拦截动作,关停账号(暂且不处理）,主叫账号加入黑名单
                if (rulePlatKeyword.getRuleAction() == 0) {
                    log.warn("[AsrDataAnalysis] {} 平台关键词：{},该关键词为拦截,执行关停流程",taskId,rulePlatKeyword.getKeyword());
                    resultTypeMap.put(STOP, true);
                    break;
                } else {
                    resultTypeMap.put(RE_CHECK, true);
                }
            }
        }
    }

    /**
     * 客户关键擦监测
     *
     * @param wordFrequencyVOList
     * @param resultTypeMap
     */
    private void customerKeyWordHandle(List<WordFrequencyVO> wordFrequencyVOList, Map<String, Boolean> resultTypeMap,String taskId) {
        List<String> ruleIdList = wordFrequencyVOList.stream().map(WordFrequencyVO::getRuleId).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ruleIdList)) {
            for (String ruleId : ruleIdList) {
                // 加redis缓存
                RuleCustomerKeywordDataBO ruleCustomerKeyword = dataService.getCustKeyword(ruleId);
                if (ruleCustomerKeyword == null || ruleCustomerKeyword.getRuleAction() == null) {
                    continue;
                }
                // 关键词拦截动作,关停账号(暂且不处理）,主叫账号加入黑名单
                if (ruleCustomerKeyword.getRuleAction() == 0) {
                    log.warn("[AsrDataAnalysis] {} 客户关键词：{},该关键词为拦截,执行关停流程",taskId,ruleCustomerKeyword.getKeyword());
                    resultTypeMap.put(STOP, true);
                    break;
                } else {
                    resultTypeMap.put(RE_CHECK, true);
                }
            }
        }
    }


}
