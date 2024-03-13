package com.ytl.vos.gateway.risk.service;

import com.alibaba.fastjson.JSONArray;
import com.ytl.vos.gateway.risk.service.bo.NlpAnalyzeRespBO;

/**
 * @author kf-zhanghui
 * @date 2023/7/31 19:26
 * NLP自然语言处理服务
 */
public interface NlpService {


    /**
     * 识别
     * @param taskId ASR任务ID
     * @param jsonArray 录音文件列表
     * @return
     */
    void nlpAnalyze(String taskId,JSONArray jsonArray);

}
