package com.ytl.vos.gateway.risk.facade.api;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author ASR渠道管理
 */
@Slf4j
@RestController
@RequestMapping("/api/asr/project")
public class AsrProjectController {

    /**
     * 是否暂停刷新ASR通道,系统初始化,不刷新通道.需要手动打开
     */
    public static AtomicBoolean stopRefreshChannel = new AtomicBoolean(true);


    @PostMapping("/refresh/{flag}")
    public String setRefreshChannelFlag(@PathVariable("flag") Integer flag) {
        if (flag == null) {
            return "Fail:参数错误\n";
        }
        if (flag == 1 && stopRefreshChannel.compareAndSet(true, false)) {
            return "Success:开始通道刷新\n";
        }
        if (flag == 0 && stopRefreshChannel.compareAndSet(false, true)) {
            return "Success:停止通道刷新\n";
        }
        return "Fail:失败\n";
    }



}
