package com.ytl.vos.gateway.risk.service.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地缓存
 *
 * @param <T>
 * @author gan.feng
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class LocalCacheBO<T> implements Serializable {
    private static final long serialVersionUID = 3090531801092438090L;
    /**
     * 数据DataBO
     */
    private T dataBO;
    /**
     * 最后加载时间
     */
    private AtomicLong lastLoadTime;
}
