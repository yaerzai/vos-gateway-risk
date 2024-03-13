package com.ytl.vos.gateway.risk.service.bo;

import lombok.Data;

/**
 * @author kf-zhanghui
 * @date 2023/8/1 10:14
 */
@Data
public class AsrContent {

    /**
     * 全部对话
     */
    private String allText;

    /**
     * 坐席通话内容
     */
    private String serviceText;


    /**
     * 客户通话内容
     */
    private String clientText;

}
