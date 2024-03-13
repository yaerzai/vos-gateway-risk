package com.ytl.vos.gateway.risk.gateway;


import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectInfoDataBO;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gan.feng
 */
@Slf4j
@Component
public class GatewayManager {
    /**
     * 所有ASR渠道信息
     */
    public static List<AsrProjectInfoDataBO> allAsrProjectInfo;


    /**
     * 静态,当前系统运行的所有ASR渠道列表
     * projectNo -> AbstractChannelGateway
     */
    public static Map<String, HttpChannelGateway> channelGateways = new ConcurrentHashMap<>();

    /**
     * ASR渠道提交队列
     * projectNo -> ChannelSubmitQueue
     */
    public static Map<String, AliYunAsrSubmitQueue> submitQueueMap = new ConcurrentHashMap<>();

    /**
     * 获取当前运行的所有网关列表
     *
     * @return
     */
    public static List<HttpChannelGateway> getAllChannelGateway() {
        return new ArrayList<>(channelGateways.values());
    }

    /**
     * 根据渠道编号获取网关信息
     *
     * @param projectNo
     * @return
     */
    public static HttpChannelGateway getChannelGateway(String projectNo) {
        return channelGateways.get(projectNo);
    }

    /**
     * 移除通道网关对象
     *
     * @param projectNo
     */
    public static void removeChannelGateway(String projectNo) {
        channelGateways.remove(projectNo);
    }


    /**
     * 获取通道提交队列
     *
     * @param projectNo 通道编号
     * @return 当通道网关对象为空时返回Null, 其他返回通道提交队列
     */
    public static synchronized AliYunAsrSubmitQueue getChannelSubmitQueue(String projectNo) {
        AliYunAsrSubmitQueue aliYunASRSubmitQueue = submitQueueMap.get(projectNo);
        if (aliYunASRSubmitQueue == null) {
            HttpChannelGateway channelGateway = channelGateways.get(projectNo);
            if (channelGateway == null) {
                log.info("[HttpGatewayManager] 初始化通道队列,但是通道网关信息不存在! {}", projectNo);
                return null;
            }
            int localMaxSize = channelGateway.getGatewayService().getGatewayServiceResource().getDataService().getSysParamInt(SysParamEnum.ASR_ALI_SUBMIT_QUEUE_MAX_SIZE);
            aliYunASRSubmitQueue = new AliYunAsrSubmitQueue(projectNo, localMaxSize, channelGateway.getGatewayService().getGatewayServiceResource().getRedisCacheService());
            submitQueueMap.put(projectNo, aliYunASRSubmitQueue);
        }
        return aliYunASRSubmitQueue;
    }

    /**
     * 获取所有通道提交队列
     *
     * @return
     */
    public static List<AliYunAsrSubmitQueue> getAllChannelSubmitQueue() {
        return new ArrayList<>(submitQueueMap.values());
    }
}
