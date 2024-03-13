package com.ytl.vos.gateway.risk.jms.consumer;

import com.ytl.common.jms.consumer.BaseJsonJmsConsumer;
import com.ytl.common.redis.service.RedisCacheService;

import com.ytl.vos.gateway.risk.gateway.GatewayManager;
import com.ytl.vos.gateway.risk.gateway.HttpChannelGateway;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.jms.code.dto.asr.AsrAliYunDTO;
import com.ytl.vos.jms.code.enums.RedisQueueEnum;
import com.ytl.vos.jms.code.enums.TopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 阿里云ASR录音文件识别
 * @author kf-zhanghui
 */
@Slf4j
@Component
public class AsrAliYunConsumer extends BaseJsonJmsConsumer<AsrAliYunDTO> {
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    protected String getTopic() {
        return TopicEnum.TOPIC_ASR_ALIYUN.getName();
    }


    @Override
    public boolean onMessage(AsrAliYunDTO message) {
        log.info("[AliYunASR] topic:{} message:{}", getTopic(), message);
        String projectNo = message.getProjectNo();
        HttpChannelGateway channelGateway = GatewayManager.getChannelGateway(projectNo);
        AliYunAsrSubmitQueue aliYunASRSubmitQueue = GatewayManager.getChannelSubmitQueue(projectNo);
        if (channelGateway == null || aliYunASRSubmitQueue == null) {
            log.warn("[AliYunASR] 厂商本地未开启/不存在本地队列,消费数据放到Redis缓存 {}", message);
            redisCacheService.lPush(RedisQueueEnum.QUEUE_ASR_ALI_SUBMIT_MESSAGE.getName() + ":" + projectNo, message);
        } else if (aliYunASRSubmitQueue.getRemoteSize() > 0) {
            log.warn("[AliYunASR] Redis队列中有数据，保证顺序，消费数据放到Redis队列中 {}", message);
            aliYunASRSubmitQueue.addRemote(message);
        } else {
            aliYunASRSubmitQueue.add(message);
        }
        return true;
    }

    @Override
    protected Class<AsrAliYunDTO> getMsgClass() {
        return AsrAliYunDTO.class;
    }
}
