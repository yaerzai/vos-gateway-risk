package com.ytl.vos.gateway.risk.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ImportAutoConfiguration(value = {com.ytl.common.redis.config.RedisClientConfig.class
        , com.ytl.common.db.config.DbConfig.class
        , com.ytl.common.jms.config.RocketJmsProperties.class
        , com.ytl.vos.persistence.aop.MyBatisConfigurer.class})
@ComponentScan({"com.ytl.common", "com.ytl.vos.persistence.dataservice","com.ytl.vos.customer.api.service.base"})
public class ComponentScanConfig {


}

