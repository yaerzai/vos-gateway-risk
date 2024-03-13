package com.ytl.vos.gateway.risk.service;


import com.ytl.vos.gateway.risk.vo.AsrProcessResultVO;

/**
 * @author lingchuanyu
 * @date 2023/7/25-16:12
 */
public interface AsrDataAnalysisService {

    /**
     * NLP质检结果处理
     * @param asrProcessResultVO
     */
    void addProcessResult(AsrProcessResultVO asrProcessResultVO);
}
