package com.ytl.vos.gateway.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum NoteMessageEnum {

    connect((byte) 1, "连接"),


    warn((byte) 2, "预警");

    /**
     * code
     */
    private byte code;

    /**
     * 消息名称
     */
    private String msg;

}
