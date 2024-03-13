package com.ytl.vos.gateway.risk.service;



import com.ytl.vos.gateway.risk.service.impl.GatewayServiceResource;
import com.ytl.vos.persistence.dataservice.bo.CallRiskProcessDataBO;


/**
 * @author yuzc
 */
public interface GatewayService {

    /**
     * DB记录Submit信息
     * @param callRiskProcessDataBO
     */
    void saveChannelSubmit(CallRiskProcessDataBO callRiskProcessDataBO);


    /**
     * 手动提交ASR
     * DB修改ASR识别taskId
     * @param callRiskProcessDataBO
     */
    void saveManualSubmit(CallRiskProcessDataBO callRiskProcessDataBO);


    /**
     * 获取外部资源封装对象
     *
     * @return
     */
    GatewayServiceResource getGatewayServiceResource();
}
