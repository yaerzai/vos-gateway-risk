package com.ytl.vos.gateway.risk.service.impl;


import com.ytl.common.redis.service.RedisCacheService;

import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.service.MonitorService;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 网关服务外部资源包
 *
 * @author gan.feng
 */
@Component
public class GatewayServiceResource {

    @Getter
    @Resource
    private RedisCacheService redisCacheService;
    @Getter
    @Resource
    private DataService dataService;
    @Getter
    @Resource
    private MonitorService monitorService;



}
