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
 * @author kf-zhanghui
 */
@Slf4j
@Component
public class ManualAsrUpdateQueue extends BatchDBRunner<CallRiskProcessDataBO> {
    @Resource
    private CallRiskProcessDataService callRiskProcessDataService;
    @Autowired
    private DataService dataService;
    @Resource
    private BatchDBRunner manualAsrUpdateQueue;

    @Override
    protected int getBatchNumber() {
        return dataService.getSysParamInt(SysParamEnum.ASR_BATCH_UPDATE_DB_NUMBER);
    }

    @Override
    protected int getRetryTimes() {
        return dataService.getSysParamInt(SysParamEnum.ASR_BATCH_UPDATE_DB_RETRY_TIMES);
    }

    @Override
    protected int getThreadNum() {
        return dataService.getSysParamInt(SysParamEnum.MANUAL_ASR_UPDATE_DB_THREAD_NUM);
    }

    @Override
    protected String getRedisQueueName() {
        return "ManualASRUpdateQueue";
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void batchDB(List<CallRiskProcessDataBO> list) {
        Map<String, List<CallRiskProcessDataBO>> map = splitTable(list);
        map.keySet().forEach(tableTime -> {
            List<CallRiskProcessDataBO> update = map.get(tableTime);
            int n = callRiskProcessDataService.batchManualAsrUpdateProcess(tableTime, update);
            if (n != update.size()) {
                log.info("[ManualASR Update Submit] DB异常,更新条数不等于队列数 更新:{}, 应该更新:{}", n, update.size());
                throw new BusinessException("[ManualASR Update Submit] DB异常,更新条数不等于队列数");
            }
        });
    }

    @Override
    protected String getTableTime(CallRiskProcessDataBO smsChannelSubmit) {
        return smsChannelSubmit.getTableTime();
    }

    @Override
    protected void handleNullFields(CallRiskProcessDataBO submit) {
        submit.setId(submit.getId() == null ? 0 : submit.getId());
        submit.setTaskId(StringUtils.defaultString(submit.getTaskId(),StringUtils.EMPTY));
        submit.setAsrRequestTime(StringUtils.defaultString(submit.getAsrRequestTime(),StringUtils.EMPTY));
        submit.setAsrStatus(submit.getAsrStatus() == null ? AsrHandleStatusEnum.FAIL.getCodeId() : submit.getAsrStatus());
        submit.setAsrErrMsg("手动ASR识别：" + StringUtils.defaultString(submit.getAsrErrMsg(),"发送ASR成功"));
        submit.setTableTime(StringUtils.defaultString(submit.getTableTime(), DateUtils.getCurrDate(DateUtils.DateFormatEnum.YYYY_MM_DD)));
    }

    @Override
    protected BatchDBRunner getInstance() {
        return manualAsrUpdateQueue;
    }
}
