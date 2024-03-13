package com.ytl.vos.gateway.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.common.redis.service.StringRedisCacheService;

import com.ytl.vos.gateway.risk.gateway.GatewayManager;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.MonitorService;
import com.ytl.vos.gateway.risk.service.bo.AsrMonitorBO;
import com.ytl.vos.jms.code.enums.RedisQueueEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author gan.feng
 */
@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {
    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private StringRedisCacheService stringRedisCacheService;
    @Autowired
    private DataService dataService;

    @Override
    public void recordLogin(String projectNo) {
        AsrMonitorBO ASRMonitorBO = getLocalMonitorBO(projectNo);
        int clientNum = ASRMonitorBO.getThreadNum().incrementAndGet();
        stringRedisCacheService.setHash(dataService.getSysParam(SysParamEnum.MONITOR_CHANNEL_CLIENT_NUM_REDIS_KEY), projectNo, String.valueOf(clientNum));
    }

    @Override
    public void recordLogout(String projectNo) {
        AsrMonitorBO ASRMonitorBO = getLocalMonitorBO(projectNo);
        int clientNum = ASRMonitorBO.getThreadNum().decrementAndGet();
        stringRedisCacheService.setHash(dataService.getSysParam(SysParamEnum.MONITOR_CHANNEL_CLIENT_NUM_REDIS_KEY), projectNo, String.valueOf(clientNum));
    }

    @Override
    public void cleanLogin(String projectNo) {
        stringRedisCacheService.delHash(dataService.getSysParam(SysParamEnum.MONITOR_CHANNEL_CLIENT_NUM_REDIS_KEY), projectNo);
    }

    @Override
    public int getTotalClientNum(String projectNo) {
        String clientNum = stringRedisCacheService.getHash(dataService.getSysParam(SysParamEnum.MONITOR_CHANNEL_CLIENT_NUM_REDIS_KEY), projectNo, String.class);
        if (StrUtil.isEmpty(clientNum)) {
            return 0;
        }
        return Integer.parseInt(clientNum);
    }

    @Override
    public int getClientNum(String projectNo) {
        AsrMonitorBO ASRMonitorBO = getLocalMonitorBO(projectNo);
        return ASRMonitorBO.getThreadNum().getAndAdd(0);
    }

    @Override
    public int getSubmitNum(String projectNo) {
        AsrMonitorBO ASRMonitorBO = getLocalMonitorBO(projectNo);
        return ASRMonitorBO.getSubmitNum().get();
    }

    @Override
    public void recordSubmit(String projectNo) {
        AsrMonitorBO ASRMonitorBO = getLocalMonitorBO(projectNo);
        ASRMonitorBO.getSubmitNum().incrementAndGet();
    }


    @Override
    public int getQueueSize(String projectNo) {
        int redisQueueSize = redisCacheService.queueSize(RedisQueueEnum.QUEUE_ASR_ALI_SUBMIT_MESSAGE.getName() + ":" + projectNo);
        int localQueueSize = 0;
        if (GatewayManager.getChannelGateway(projectNo) != null) {
            AliYunAsrSubmitQueue aliYunASRSubmitQueue = GatewayManager.getChannelSubmitQueue(projectNo);
            localQueueSize = aliYunASRSubmitQueue != null ? aliYunASRSubmitQueue.getSize() : 0;
        }
        return redisQueueSize + localQueueSize;
    }

    /**
     * 获取监控信息
     *
     * @param projectNo
     * @return
     */
    protected AsrMonitorBO getLocalMonitorBO(String projectNo) {
        AsrMonitorBO ASRMonitorBO = localMonitors.get(projectNo);
        if (ASRMonitorBO == null) {
            ASRMonitorBO = new AsrMonitorBO(projectNo);
        }
        localMonitors.put(projectNo, ASRMonitorBO);
        return ASRMonitorBO;
    }
}
