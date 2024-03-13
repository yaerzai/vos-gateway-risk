package com.ytl.vos.gateway.risk.service.bo;

import lombok.*;

import java.io.Serializable;

/**
 * @author kf-zhanghui
 * @date 2023/07/25 18:34
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HttpSendRequestBO implements Serializable {

    private static final long serialVersionUID = 1021824500526283765L;

    /**
     * 话单ID
     */
    private String callId;

    /**
     * 项目编号
     */
    private String projectNo;

    /**
     * 账号ID
     */
    private String accessKeyId;

    /**
     * 账号密码
     */
    private String accessKeySecret;

    /**
     * 识别应用ID
     */
    private String appKey;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 录音文件地址
     */
    private String voiceAddress;

}
