package com.ytl.vos.gateway.risk.task;

import com.ytl.common.base.controller.SystemController;
import com.ytl.common.base.service.BaseService;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.queue.runner.BatchDBRunner;
import com.ytl.vos.gateway.risk.gateway.GatewayManager;
import com.ytl.vos.gateway.risk.queue.bo.AliYunAsrSubmitQueue;
import com.ytl.vos.gateway.risk.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author kf-zhanghui
 */
@Slf4j
@Component
public class ChannelMonitorTask {
    @Autowired
    private Map<String, BatchDBRunner> batchDBRunnerMap;
    @Resource
    private BaseService smsBaseService;
    @Resource
    private MonitorService monitorService;
    /**
     * 10秒打印一次监控信息
     */
    @Scheduled(fixedDelay = 10 * 1000)
    public void log() {
        List<Map.Entry<String, BatchDBRunner>> collect = batchDBRunnerMap.entrySet().stream().filter(bacthDBRunner -> bacthDBRunner.getValue().getQueueSize() > 0).collect(Collectors.toList());
        String logPrefix = "[Monitor Message]";
        StringBuilder stringBuilder = new StringBuilder(logPrefix);
        if (!CollectionUtils.isEmpty(collect)) {
            stringBuilder.append("批量DB队列大小");
            collect.forEach(item -> stringBuilder.append(" ").append(item.getKey()).append(":").append(item.getValue().getQueueSize()));
        }
        String logStr = stringBuilder.toString();
        if (!logPrefix.equals(logStr)) {
            log.warn(logStr);
        }
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void monitor() {
        if (!SystemController.getSystemOpenFlag()) {
            return;
        }
        Date currDate = DateUtils.now();
        MonitorService.localMonitors.values().forEach(item -> {
            String date = DateUtils.getDateStr(currDate, DateUtils.DateFormatEnum.YYYY_MM_DD_HH_MM_SS);
            String projectNo = item.getProjectNo();

            int submitNum = item.getSubmitNum().getAndSet(0);
            int submitSuccessNum = item.getSubmitSuccessNum().getAndSet(0);
            int submitFailNum = item.getSubmitFailNum().getAndSet(0);

            Integer nodeId = smsBaseService.getNodeId();
            int threadNum = item.getThreadNum().get();
            int totalClientNum = monitorService.getTotalClientNum(projectNo);
            if (projectNo.contains("Server")) {
                log.warn("[ChannelMonitor] {}({})-->连接数:{}/{}", projectNo, nodeId, threadNum, totalClientNum);
                return;
            }


            int queueSize = monitorService.getQueueSize(projectNo);
            int localQueueSize = 0;
            if (GatewayManager.getChannelGateway(projectNo) != null) {
                AliYunAsrSubmitQueue aliYunASRSubmitQueue = GatewayManager.getChannelSubmitQueue(projectNo);
                localQueueSize = aliYunASRSubmitQueue != null ? aliYunASRSubmitQueue.getSize() : 0;
            }
            if (submitNum  + queueSize <= 0) {
                return;
            }

            String logStr = projectNo + "(" + nodeId + ")-->主线程数:" + threadNum + "/" + totalClientNum + " " + date + " 提交数:" + submitNum
                    + "(" + submitSuccessNum + "/" + submitFailNum + ")"
                    + ",本地队列大小:" + localQueueSize + ",远程队列大小:" + (queueSize - localQueueSize);
            log.warn("[ChannelMonitor] {}", logStr);
        });
    }
}
