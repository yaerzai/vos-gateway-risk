package com.ytl.vos.gateway.risk.queue.bo;


import com.ytl.common.queue.LocalQueue;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.vos.jms.code.dto.asr.AsrAliYunDTO;
import com.ytl.vos.jms.code.enums.RedisQueueEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 通道提交队列
 *
 * @author gan.feng
 */
@Slf4j
public class AliYunAsrSubmitQueue extends LocalQueue<AsrAliYunDTO> {
    /**
     * 项目编号
     */
    @Getter
    private String projectNo;

    public AliYunAsrSubmitQueue(String channelNo, int localMaxSize, RedisCacheService redisCacheService) {
        super(localMaxSize, redisCacheService);
        this.projectNo = channelNo;
    }

    @Override
    public void addRemote(AsrAliYunDTO item) {
        redisCacheService.rPush(getRedisQueueKey(), item);
    }

    @Override
    public AsrAliYunDTO getRemote() {
        return redisCacheService.lPop(getRedisQueueKey(), AsrAliYunDTO.class);
    }

    @Override
    public int getRemoteSize() {
        return redisCacheService.queueSize(getRedisQueueKey());
    }

    @Override
    protected void outMaxProcess(AsrAliYunDTO item) {
        log.warn("[AliYunASR] 本地队列已达到最大值 {}/{},消费数据放到Redis缓存", getSize(), localMaxSize);
        addRemote(item);
    }


    public String getRedisQueueKey() {
        return RedisQueueEnum.QUEUE_ASR_ALI_SUBMIT_MESSAGE.getName() + ":" + projectNo;
    }
}
