package com.ytl.vos.gateway.risk.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author kf-zhanghui
 * @date 2023/8/10 14:28
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VoiceText {

    /**
     * 全文内容
     */
    private StringBuilder allText = new StringBuilder();

    /**
     * 坐席内容
     */
    private StringBuilder serviceText = new StringBuilder();

    /**
     * 客户内容
     */
    private StringBuilder clientText = new StringBuilder();

}
