package com.ytl.vos.gateway.risk.constant;


import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 系统内部静态类
 * @author kf-zhanghui
 */
public class SysConstant {

    public static final ThreadPoolExecutor warnSendExecutors = new ThreadPoolExecutor(5, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("warnSend"));

    /**
     * 提交线程池
     */
    public static final ThreadPoolExecutor submitExecutors = new ThreadPoolExecutor(100, 100, 10L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("ASRSubmit"));


    /**
     * NLP质检处理线程池
     */
    public static final ThreadPoolExecutor nlpHandle = new ThreadPoolExecutor(100, 100, 10L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("NLP"));


    public static final String NLP_SOURCE_PLATFORM= "PLATFORM";

    public static final String NLP_SOURCE_CUSTOMER= "CUSTOMER";

    static {
        // 核心线程允许超时释放
        submitExecutors.allowCoreThreadTimeOut(true);
        nlpHandle.allowCoreThreadTimeOut(true);
    }

}
