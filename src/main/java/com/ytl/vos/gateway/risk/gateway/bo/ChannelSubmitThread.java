package com.ytl.vos.gateway.risk.gateway.bo;


import com.ytl.vos.gateway.risk.gateway.HttpChannelGateway;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author yuzc
 */
@Slf4j
@Builder
@Setter
@Getter
@ToString
public class ChannelSubmitThread implements Serializable {
    private static final long serialVersionUID = -3093062634825511709L;
    /**
     * 线程
     */
    private Runnable thread;

    /**
     * 通道网关信息
     */
    private HttpChannelGateway channelGateway;
}
