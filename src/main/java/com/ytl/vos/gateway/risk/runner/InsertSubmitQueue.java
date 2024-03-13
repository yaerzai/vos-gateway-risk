package com.ytl.vos.gateway.risk.runner;


import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.queue.runner.BatchDBRunner;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.persistence.dataservice.CallRiskProcessDataService;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;
import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


/**
 * @author gan.feng
 */
@Slf4j
@Component
public class InsertSubmitQueue extends BatchDBRunner<CallRiskProcessDataBO> {
    @Resource
    private CallRiskProcessDataService callRiskProcessDataService;
    @Autowired
    private DataService dataService;
    @Resource
    private BatchDBRunner insertSubmitQueue;

    @Override
    protected int getBatchNumber() {
        return dataService.getSysParamInt(SysParamEnum.ASR_BATCH_INSERT_DB_NUMBER);
    }

    @Override
    protected int getRetryTimes() {
        return dataService.getSysParamInt(SysParamEnum.ASR_BATCH_INSERT_DB_RETRY_TIMES);
    }

    //插入队列指定线程数，不使用刷新的方式
    @Override
    protected int getThreadNum() {
        return dataService.getSysParamInt(SysParamEnum.ASR_BATCH_INSERT_DB_THREAD_NUM);
    }

    @Override
    protected String getRedisQueueName() {
        return "ASRInsertSubmitQueue";
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void batchDB(List<CallRiskProcessDataBO> list) {
        Map<String, List<CallRiskProcessDataBO>> map = splitTable(list);
        map.keySet().forEach(tableTime -> {
            List<CallRiskProcessDataBO> insert = map.get(tableTime);
            int n = callRiskProcessDataService.batchInsertCallRiskProcess(tableTime, insert);
            if (n != insert.size()) {
                log.info("[Insert Submit] DB异常,插入条数不等于队列数 插入:{}, 应该插入:{}", n, insert.size());
                throw new BusinessException("[Insert Submit] DB异常,插入条数不等于队列数");
            }
        });
    }

    @Override
    protected String getTableTime(CallRiskProcessDataBO smsChannelSubmit) {
        return smsChannelSubmit.getTableTime();
    }

    @Override
    protected void handleNullFields(CallRiskProcessDataBO submit) {
        submit.setPlanId(StringUtils.defaultString(dataService.getPlanId(submit.getUserNo()), StringUtils.EMPTY));
        submit.setTaskId(StringUtils.defaultString(submit.getTaskId(),StringUtils.EMPTY));
        submit.setCallId(StringUtils.defaultString(submit.getCallId(),StringUtils.EMPTY));
        submit.setPlatCallId(StringUtils.defaultString(submit.getPlatCallId(),StringUtils.EMPTY));
        submit.setCustomerNo(StringUtils.defaultString(submit.getCustomerNo(),StringUtils.EMPTY));
        submit.setUserNo(StringUtils.defaultString(submit.getUserNo(),StringUtils.EMPTY));
        submit.setFileUrl(StringUtils.defaultString(submit.getFileUrl(),StringUtils.EMPTY));
        submit.setAsrRequestTime(StringUtils.defaultString(submit.getAsrRequestTime(),StringUtils.EMPTY));
        submit.setAsrStatus(submit.getAsrStatus() == null ? AsrHandleStatusEnum.FAIL.getCodeId():submit.getAsrStatus());
        submit.setAsrErrMsg(StringUtils.defaultString(submit.getAsrErrMsg(),StringUtils.EMPTY));
        submit.setAsrDoneTime(StringUtils.defaultString(submit.getAsrDoneTime(),StringUtils.EMPTY));
        submit.setNlpDoneTime(StringUtils.defaultString(submit.getNlpDoneTime(),StringUtils.EMPTY));
        submit.setAsrText(StringUtils.defaultString(submit.getAsrText(),StringUtils.EMPTY));
        submit.setHitSysRules(StringUtils.defaultString(submit.getHitSysRules(),StringUtils.EMPTY));
        submit.setHitUserRules(StringUtils.defaultString(submit.getHitUserRules(),StringUtils.EMPTY));
        submit.setNlpLabel(StringUtils.defaultString(submit.getNlpLabel(),StringUtils.EMPTY));
        submit.setNlpScore(StringUtils.defaultString(submit.getNlpScore(),StringUtils.EMPTY));
        submit.setNlpResult(submit.getNlpResult() == null ? 0 : submit.getNlpResult());
        submit.setNlpDesc(StringUtils.defaultString(submit.getNlpDesc(),StringUtils.EMPTY));
        submit.setTableTime(StringUtils.defaultString(submit.getTableTime(), DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD)));
    }

    @Override
    protected BatchDBRunner getInstance() {
        return insertSubmitQueue;
    }
}
