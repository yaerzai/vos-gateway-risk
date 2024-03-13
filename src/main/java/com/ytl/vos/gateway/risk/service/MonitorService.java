package com.ytl.vos.gateway.risk.service;



import com.ytl.vos.gateway.risk.service.bo.AsrMonitorBO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控服务接口
 *
 * @author gan.feng
 */
public interface MonitorService {
    /**
     * 监控信息
     */
    Map<String, AsrMonitorBO> localMonitors = new ConcurrentHashMap<>();

    /**
     * 增加处理队列主线程数量
     * @param projectNo
     */
    void recordLogin(String projectNo);

    /**
     * 减少处理队列线程数量
     * @param projectNo
     */
    void recordLogout(String projectNo);

    /**
     * 清空ASR队列的消耗线程
     *
     * @param projectNo
     */
    void cleanLogin(String projectNo);

    /**
     * 获取ASR队列所有线程数
     * @param projectNo
     * @return
     */
    int getTotalClientNum(String projectNo);

    /**
     * 获取本地线程数
     *
     * @param projectNo
     * @return
     */
    int getClientNum(String projectNo);

    /**
     * 获取当前周期平均每秒提交数
     * @param projectNo
     * @return
     */
    int getSubmitNum(String projectNo);

    /**
     * 记录提交
     *
     * @param projectNo
     */
    void recordSubmit(String projectNo);



    /**
     * 待提交队列大小
     *
     * @param projectNo
     * @return 本地队列+远程队列
     */
    int getQueueSize(String projectNo);
}
