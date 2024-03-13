package com.ytl.vos.gateway.risk.service.impl;


import com.ytl.vos.gateway.risk.constant.RedisKeysConstant;
import com.ytl.vos.gateway.risk.runner.InsertSubmitQueue;
import com.ytl.vos.gateway.risk.runner.ManualAsrUpdateQueue;
import com.ytl.vos.gateway.risk.service.GatewayService;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;
import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * @author kf-zhanghui
 */
@Slf4j
@Service
public class GatewayServiceImpl implements GatewayService {
    @Resource
    private InsertSubmitQueue insertSubmitQueue;
    @Resource
    private ManualAsrUpdateQueue manualAsrUpdateQueue;
    @Getter
    @Autowired
    private GatewayServiceResource gatewayServiceResource;

    @Override
    public void saveChannelSubmit(CallRiskProcessDataBO callRiskProcessDataBO) {
        insertSubmitQueue.add(callRiskProcessDataBO);
    }

    @Override
    public void saveManualSubmit(CallRiskProcessDataBO callRiskProcessDataBO) {
        manualAsrUpdateQueue.add(callRiskProcessDataBO);
        if(AsrHandleStatusEnum.FAIL.getCodeId() != callRiskProcessDataBO.getAsrStatus()){
            gatewayServiceResource.getRedisCacheService().setHash(RedisKeysConstant.MANUAL_ASR_TABLE_TIME,
                    callRiskProcessDataBO.getTaskId(),callRiskProcessDataBO.getTableTime(),24*60*60);
        }
    }

}
