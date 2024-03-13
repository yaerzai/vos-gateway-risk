package com.ytl.vos.gateway.risk.service.bo;

import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import lombok.*;

import java.io.Serializable;

/**
 * @author kf-zhanghui
 * @date 2023/07/26 10:03
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HttpSendResponseBO implements Serializable {

    private static final long serialVersionUID = -8487664780952233014L;
    /**
     * 处理结果：0-未知,1-成功,2-失败
     */
    private AsrHandleStatusEnum respResult;
    /**
     * 错误描述
     */
    private String msg;

    /**
     * 请求ID流水，仅用于联调
     */
    private String requestId;

    /**
     * 识别任务ID
     */
    private String taskId;



    public AsrRecognizeResultBO buildDataBO() {
        AsrRecognizeResultBO dataBO = new AsrRecognizeResultBO();
        return dataBO;
    }
}
