package com.ytl.vos.gateway.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author lingchuanyu
 * @date 2023/7/26-10:55
 */
@Getter
@AllArgsConstructor
public enum AsrRuleEnum {

    KEY_WORD("keyword","关键词"),

    EMOTION("emotion","情绪"),

    VERBAL_TRICK("trick","话术");



    /**
     * code
     */
    private final String code;

    /**
     * 中文名称
     */
    private final String zName;
}
