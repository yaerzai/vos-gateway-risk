package com.ytl.vos.gateway.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author lingchuanyu
 * @date 2023/8/1-10:55
 */
@Getter
@AllArgsConstructor
public enum NotifyTypeEnum {

    WECHAT("0","公众号"),

    SMS("1","短信"),

    EMAIL("2","邮件"),

    PLATFORM("3","平台消息");


    /**
     * code
     */
    private final String code;

    /**
     * 中文名称
     */
    private final String zName;


    /**
     * 解析
     * @param code 状态码
     * @return
     */
    public static NotifyTypeEnum parse(String code) {
        return Arrays.stream(values()).filter(item-> item.code.equals(code)).findAny().orElse(null);
    }
}
