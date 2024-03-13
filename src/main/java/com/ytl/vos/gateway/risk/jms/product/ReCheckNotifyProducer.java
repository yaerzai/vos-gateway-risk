package com.ytl.vos.gateway.risk.jms.product;

import com.ytl.common.jms.producer.BaseJmsProducer;
import com.ytl.common.jms.producer.BaseJsonJmsProducer;
import com.ytl.vos.gateway.risk.dto.PlatformNotifyDTO;
import com.ytl.vos.jms.code.enums.TopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 复核消息通知生产者
 *
 * @author lingchuanyu
 * @date 2023/8/2-10:36
 */
@Slf4j
@Component
public class ReCheckNotifyProducer extends BaseJsonJmsProducer<PlatformNotifyDTO> {

    @Override
    protected String getTopic() {
        return TopicEnum.TOPIC_RISK_RE_CHECK_NOTIFY.getName();
    }
}
