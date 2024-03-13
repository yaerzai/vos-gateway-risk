package com.ytl.vos.gateway.risk.queue.task;

import com.ytl.common.base.controller.SystemController;
import com.ytl.common.queue.QueueService;

import com.ytl.vos.gateway.risk.gateway.GatewayManager;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 队列拉取定时任务
 *
 * @author gan.feng
 */
@Slf4j
@Component
public class LocalQueueServiceTask {

    @Resource(name = "submitQueueService")
    private QueueService<AliYunAsrSubmitQueue> queueService;

    /**
     * 定时任务,10s执行一次,判断Redis队列中是否有值,如果有值就拉取到本地
     */
    @Scheduled(fixedDelay = 1000 * 10)
    public void submitQueue() {
        GatewayManager.getAllChannelSubmitQueue().forEach(submitQueue -> {
            if (SystemController.getSystemOpenFlag() && GatewayManager.getChannelGateway(submitQueue.getProjectNo()) != null) {
                queueService.remoteToLocal(submitQueue);
            } else {
                queueService.localToRemote(submitQueue);
            }
        });
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void monitor() {
        GatewayManager.getAllChannelSubmitQueue().forEach(submitQueue -> {
            String projectNo = submitQueue.getProjectNo();
            int localSize = submitQueue.getSize();
            int redisQueueSize = submitQueue.getRemoteSize();
            int maxSize = submitQueue.getLocalMaxSize();
            if (localSize + redisQueueSize > 0) {
                log.warn("[ASRQueueMonitor] ASR渠道编号{} 本地队列:{}, Redis队列:{}，本地队列最大值:{}", projectNo, localSize, redisQueueSize, maxSize);
            }
        });
    }
}
