package com.ytl.vos.gateway.risk.queue.impl;


import com.ytl.common.queue.QueueService;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.jms.code.dto.asr.AsrAliYunDTO;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author gan.feng
 */
@Slf4j
@Service("submitQueueService")
public class SubmitQueueServiceImpl implements QueueService<AliYunAsrSubmitQueue> {
    @Resource
    private DataService dataService;

    @Override
    public void localToRemote(AliYunAsrSubmitQueue submitQueue) {
        String projectNo = submitQueue.getProjectNo();
        int localSize = submitQueue.getSize();
        if (localSize == 0) {
            log.info("[AliYunAsrSubmitQueue] 本地->Redis 本地队列为空 厂商项目编号:{} 本地队列:{} ", projectNo, localSize);
            return;
        }

        AsrAliYunDTO asrAliYunDTO = submitQueue.get();
        int num = 0;
        while (asrAliYunDTO != null) {
            submitQueue.addRemote(asrAliYunDTO);
            asrAliYunDTO = submitQueue.get();
            num++;
        }
        if (num > 0) {
            log.warn("[AliYunAsrSubmitQueue] 本地->Redis 厂商项目编号:{} 本地队列:{} Redis队列:{} 上传数量:{}", projectNo, submitQueue.getSize(), submitQueue.getRemoteSize(), num);
        }
    }

    @Override
    public void remoteToLocal(AliYunAsrSubmitQueue submitQueue) {
        String projectNo = submitQueue.getProjectNo();
        int localQueueMaxSize = submitQueue.getLocalMaxSize() * 9 / 10;
        if (submitQueue.getSize() >= localQueueMaxSize || submitQueue.getRemoteSize() == 0) {
            return;
        }
        // 本地队列最大值,系统参数控制，默认1000000
        submitQueue.setLocalMaxSize(dataService.getSysParamInt(SysParamEnum.ASR_ALI_SUBMIT_QUEUE_MAX_SIZE));
        int num = 0;
        while (submitQueue.getSize() < localQueueMaxSize) {
            AsrAliYunDTO asrAliYunDTO = submitQueue.getRemote();
            if (asrAliYunDTO == null) {
                break;
            }
            submitQueue.addLocal(asrAliYunDTO);
            num++;
        }
        if (num > 0) {
            log.warn("[AliYunAsrSubmitQueue] Redis->本地ASR渠道编号:{} 本地队列:{} Redis队列:{} 加载数量:{}", projectNo, submitQueue.getSize(), submitQueue.getRemoteSize(), num);
        }
    }
}
