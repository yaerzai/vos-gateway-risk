package com.ytl.vos.gateway.risk.service.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kf-zhanghui
 * @date 2023/7/31 19:28
 * NLP服务分析结果对象
 */

@Data
public class NlpAnalyzeRespBO {

    /**
     * 平台关键词列表
     */
    private List<KeyWord> platKeywordList = new ArrayList<>();

    /**
     * 客户关键词列表
     */
    private List<KeyWord> customerKeywordList = new ArrayList<>();

    /**
     * 平台话术列表
     */
    private List<KeyWord> platScripList = new ArrayList<>();

    /**
     * 客户话术列表
     */
    private List<KeyWord> customerScripList = new ArrayList<>();
}
