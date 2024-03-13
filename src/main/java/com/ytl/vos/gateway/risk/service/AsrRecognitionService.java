package com.ytl.vos.gateway.risk.service;


import com.ytl.vos.gateway.risk.service.bo.HttpSendRequestBO;
import com.ytl.vos.gateway.risk.service.bo.HttpSendResponseBO;

/**
 * @author kf-zhanghui
 * @date 2023/7/25 16:59
 * ASR 录音文件识别接口
 */
public interface AsrRecognitionService {


    /**
     * 录音文件识别
     * @param httpSendRequestBO
     * @return
     */
    HttpSendResponseBO send(HttpSendRequestBO httpSendRequestBO);

    /***
     * 录音文件识别回调结果
     * @param body 回调body
     */
    void callback(String body);
}
