package com.ytl.vos.gateway.risk.service.bo;

import lombok.*;

import java.io.Serializable;

/**
 * @author kf-zhanghui
 * @date 2023/7/26 11:52
 * ASR识别结果对象
 */

@Builder
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AsrRecognizeResultBO implements Serializable {
    private static final long serialVersionUID = -6050846726145066265L;

    /**
     * 厂商编号
     */
    private String projectNo;
    /**
     * 话单ID
     */
    private String callId;

    /**
     * 识别任务ID
     */
    private String taskId;

    /**
     * 客户号
     */
    private String customerNo;

    /**
     * 客户账号
     */
    private String userNo;

    /**
     * 请求时间
     */
    private String requestTime;

    /**
     * 方案ID
     */
    private String optionId;

    /**
     * 文件地址
     */
    private String fileAddress;

    /**
     * 状态
     */
    private Byte status;

    /**
     * 失败描述
     */
    private String errorMsg;


    /**
     * 方案名称
     */
    private String optionName;


    private String tableTime;

}
