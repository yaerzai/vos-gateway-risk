package com.ytl.vos.gateway.risk.task;

import cn.hutool.json.JSONUtil;
import com.ytl.common.base.controller.SystemController;
import com.ytl.vos.gateway.risk.facade.api.AsrProjectController;
import com.ytl.vos.gateway.risk.gateway.GatewayManager;
import com.ytl.vos.gateway.risk.gateway.HttpChannelGateway;
import com.ytl.vos.gateway.risk.service.AsrRecognitionService;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.GatewayService;
import com.ytl.vos.gateway.risk.service.MonitorService;
import com.ytl.vos.gateway.risk.service.bo.AsrMonitorBO;
import com.ytl.vos.persistence.dataservice.AsrProjectInfoDataService;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectInfoDataBO;
import com.ytl.vos.persistence.dataservice.bo.AsrProjectInfoQueryBO;
import com.ytl.vos.persistence.enums.RecordStatusEnum;
import com.ytl.vos.persistence.enums.StatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ganfeng
 * @date 2019/1/22
 */
@Slf4j
@Component
public class GatewayRefreshTask {
    @Resource
    private AsrProjectInfoDataService asrProjectInfoDataService;
    @Resource
    private GatewayService gatewayService;
    @Resource
    private MonitorService monitorService;
    @Resource
    private DataService dataService;
    @Resource
    private AsrRecognitionService asrRecognitionService;

    /**
     * 每分钟查询库中所有通道编号
     */
    @Scheduled(fixedDelay = 1000 * 60)
    public void refreshAllChannelNo() {
        AsrProjectInfoQueryBO queryReqDTO = AsrProjectInfoQueryBO.builder().status(StatusEnum.ENABLE.getCodeId()).build();
        List<AsrProjectInfoDataBO> newAsrProject = asrProjectInfoDataService.query(queryReqDTO).getData();

        if (!CollectionUtils.isEmpty(GatewayManager.allAsrProjectInfo)) {
            GatewayManager.allAsrProjectInfo.forEach(item -> {
                if (newAsrProject.stream().noneMatch(project -> project.getProjectNo().equals(item.getProjectNo()))) {
                    log.warn("[ASR Gateway Refresh] 通道已剔除，需要移除 {}", item.getProjectNo());
                    GatewayManager.removeChannelGateway(item.getProjectNo());
                    monitorService.cleanLogin(item.getProjectNo());
                }
            });
        }

        GatewayManager.allAsrProjectInfo = newAsrProject;
        log.warn("[ASR Gateway Refresh] 当前厂商信息:{}", GatewayManager.getAllChannelGateway().stream().map(HttpChannelGateway::info).collect(Collectors.toList()));
        if (!SystemController.getSystemOpenFlag()) {
            AsrProjectController.stopRefreshChannel.compareAndSet(false, true);
            GatewayManager.getAllChannelGateway().forEach(channelGateway -> {
                String projectNo = channelGateway.getAsrProjectInfoDataBO().getProjectNo();
                log.warn("[ASRGateway Refresh] 系统已关闭-关停ASR项目 {}", projectNo);
                GatewayManager.removeChannelGateway(projectNo);
                monitorService.cleanLogin(projectNo);
            });
        }
    }

    /**
     * 每10秒 检查连接
     */
    @Scheduled(fixedDelay = 1000 * 10)
    public void refreshConnection() {
        if (AsrProjectController.stopRefreshChannel.get()) {
            log.warn("[ASR Gateway Refresh] 刷新ASR通道开关已关闭");
            return;
        }

        if (CollectionUtils.isEmpty(GatewayManager.allAsrProjectInfo)) {
            log.warn("[ASR Gateway Refresh] 系统所有ASR通道信息未加载,退出!");
            return;
        }

        GatewayManager.allAsrProjectInfo.forEach(asrProject -> {
            String projectNo = asrProject.getProjectNo();
            AsrProjectInfoDataBO asrProjectInfo = dataService.getAsrProjectInfo(projectNo);
            if (asrProjectInfo == null || RecordStatusEnum.OK.getByteCodeId() != asrProjectInfo.getStatus()) {
                GatewayManager.removeChannelGateway(projectNo);
                monitorService.cleanLogin(projectNo);
                return;
            }

            HttpChannelGateway channelGateway = GatewayManager.getChannelGateway(projectNo);
            if (channelGateway == null) {
                channelGateway = new HttpChannelGateway(asrProject);
                log.warn("[ASR Gateway Refresh] 新增ASR渠道 ASR-PROJECT: {}", JSONUtil.toJsonStr(asrProject));
                channelGateway.setGatewayService(gatewayService);
                channelGateway.setAsrRecognitionService(asrRecognitionService);
                GatewayManager.channelGateways.put(projectNo, channelGateway);
                log.warn("[ASR Gateway Refresh] 重置ASR渠道监控信息");
                MonitorService.localMonitors.put(projectNo, new AsrMonitorBO(projectNo));
            } else {
                // 更新渠道配置参数
                channelGateway.setAsrProjectInfoDataBO(asrProjectInfo);
                log.debug("[ASR Gateway Refresh] 更新ASR渠道信息: channelInfo: {}" + JSONUtil.toJsonStr(asrProjectInfo));
            }
            int clientNum = monitorService.getClientNum(projectNo);
            int threadNum = channelGateway.getSubmitThreads().size();
            int queueSize = monitorService.getQueueSize(projectNo);
            log.warn("[ASR Gateway Refresh] {} 线程数:{} 队列积压:{}", asrProjectInfo.getProjectNo(),threadNum, queueSize);

            if (clientNum == 0) {
                log.info("[ASR Gateway Refresh] {} 当前无线程,创建线程，队列积压:{}", projectNo, queueSize);
                channelGateway.createSocketChannel();
                return;
            }
            int maxThreadNum = dataService.getSysParamInt(SysParamEnum.ASR_ALI_PROJECT_MAX_THREAD_NUM);
            if (clientNum >= maxThreadNum) {
                log.info("[ASR Gateway Refresh] {} 已创建所有主线程{}/{}，不可再创建线程，队列积压:{}", projectNo, clientNum, maxThreadNum, queueSize);
                return;
            }
            // 当队列有积压或者不控制时，创建新连接
            int delayQueueSize = dataService.getSysParamInt(SysParamEnum.ASR_ALI_PROJECT_DELAY_QUEUE_SIZE);
            if (delayQueueSize == 0 || queueSize >= delayQueueSize) {
                log.warn("[ASR Gateway Refresh] {} 队列有积压{}/{}，创建新线程，当前线程数:{}/{}", projectNo, queueSize, delayQueueSize, clientNum, maxThreadNum);
                channelGateway.createSocketChannel();
            }
        });
    }

}
