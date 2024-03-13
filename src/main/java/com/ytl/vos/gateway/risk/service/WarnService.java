package com.ytl.vos.gateway.risk.service;

import com.ytl.vos.persistence.dataservice.bo.PlatNoteConfigDataBO;
import com.ytl.vos.persistence.enums.MonitorWarnTypeEnum;

/**
 * @author lingchuanyu
 * @date 2023/7/31-16:02
 */
public interface WarnService {

    /**
     * 发送预警
     *
     * @param warnType    预警类型
     * @param warnContent 预警内容
     */
    void sendWarn(PlatNoteConfigDataBO warnType, String warnContent);
}
