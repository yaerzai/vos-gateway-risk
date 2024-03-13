package com.ytl.vos.gateway.risk.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户监控对象
 *
 * @author ganfeng
 * @date 2019/1/22
 */
@Getter
@Setter
@ToString
public class AsrMonitorBO implements Serializable {
    private static final long serialVersionUID = -3679528415546407626L;
    /**
     * ASR项目编号
     */
    private String projectNo;
    /**
     * 消耗队列线程数
     */
    private AtomicInteger threadNum;
    /**
     * Submit提交数
     */
    private AtomicInteger submitNum;

    /**
     * Submit提交成功数
     */
    private AtomicInteger submitSuccessNum;
    /**
     * Submit提交失败数
     */
    private AtomicInteger submitFailNum;


    public AsrMonitorBO(String projectNo) {
        this.projectNo = projectNo;
        this.threadNum = new AtomicInteger();
        this.submitNum = new AtomicInteger();
        this.submitSuccessNum = new AtomicInteger();
        this.submitFailNum = new AtomicInteger();
    }
}
