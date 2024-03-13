package com.ytl.vos.gateway.risk.vo;

import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lingchuanyu
 * @date 2023/7/26-10:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordFrequencyVO {

    /**
     * 词id
     */
    private String ruleId;

    /**
     * 词名称
     */
    private String name;

    /**
     * 次数
     */
    private Long number;

    /**
     * 分词匹配情况
     */
    private List<KeyWord.SpiltWord> spiltWords = new ArrayList<>();

    /**
     * 享尽此匹配情况
     */
    private Map<String,List<KeyWord.SynonymWord>> synonymWords = new HashMap<>();
}
