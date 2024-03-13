package com.ytl.vos.gateway.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendReqDTO {

    private static final long serialVersionUID = -4836170351953307403L;

    /**
     * 客户账号
     */
    private String cid;

    /**
     * 时间错
     */
    private String timeStamp;

    /**
     * 签名
     */
    private String sign;

    /**
     * 短信内容
     */
    private String content;

    /**
     * 发送手机号列表
     */
    private String mobileNo;

}
