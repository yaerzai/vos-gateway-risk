package com.ytl.vos.gateway.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author lingchuanyu
 * @date 2023/8/2-10:41
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformNotifyDTO implements Serializable {

    private static final long serialVersionUID = -6677078854965221851L;

    /**
     * 发送内容
     */
    private String warnContent;

    /**
     * 通知客户号
     */
    private String userNo;

    /**
     * 通知id
     */
    private Long msgId;
}
