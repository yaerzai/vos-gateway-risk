package com.ytl.vos.gateway.risk;

import cn.hutool.core.util.StrUtil;
import com.hankcs.hanlp.utility.Predefine;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
@EnableFeignClients({"com.ytl"})
@MapperScan(value = {"com.ytl.vos.persistence.mobile.mapper", "com.ytl.vos.persistence.mapper","com.ytl.vos.base.common.persistence.mapper"})
public class Application {

    public static void main(String[] args) {

        String hanLpPath = System.getProperty("hanlp_properties_path");
        //读取设置的hanlp.properties路径
        if(StrUtil.isNotEmpty(hanLpPath)){
            Predefine.HANLP_PROPERTIES_PATH = hanLpPath;
        }

        SpringApplication.run(Application.class, args);
    }

}
