package com.ytl.vos.gateway.risk.service.bo;

import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kf-zhanghui
 * @date 2023/7/31 19:33
 * 关键词
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyWord {

    /**
     * 词ID
     */
    private String ruleId;
    /**
     * 关键词赐名
     */
    private String name;

    /**
     * 关键词来源
     */
    private String source;

    /**
     * 关键词标签
     */
    private String tag;

    /**
     * 分数
     */
    private Integer score = 0;

    /**
     * 分词匹配情况
     */
    private List<SpiltWord> spiltWords = new ArrayList<>();

    /**
     * 享尽此匹配情况
     */
    private Map<String,List<SynonymWord>> synonymWords = new HashMap<>();

    /**
     * 近似词
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynonymWord {
        private String name;

        private Integer times;

        private Integer score;
    }

    /**
     * 分词类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpiltWord {
        private String name;

        private Integer times;

        private Integer score;
    }
}
