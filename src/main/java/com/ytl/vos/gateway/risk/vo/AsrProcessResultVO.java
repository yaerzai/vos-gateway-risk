package com.ytl.vos.gateway.risk.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author lingchuanyu
 * @date 2023/7/25-15:44
 */

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AsrProcessResultVO implements Serializable {


    private static final long serialVersionUID = -8954322826123108916L;

    @NotNull(message = "id")
    private Long asrProcessId;

    /**
     * 话单id
     */
    @NotNull(message = "callid不能为空")
    private String callId;

    /**
     * 方案ID
     */
    private  String planId;

    /**
     * 录音文件存储路径
     */
    private  String fileUrl;

    /**
     * 任务关联id
     */
    @NotNull(message = "任务关联id不能为空")
    private String taskId;

     /**
     * 客户账号
     */
     @NotNull(message = "客户账号不能为空")
     private String userNo;

    /**
     * 客户号
     */
    @NotNull(message = "客户号不能为空")
    private String customerNo;

    /**
     * 请求时间
     */
    private String asrRequestTime;

    /**
     * callId 入库的分表日期
     */
    @NotNull(message = "分表日期不能为空")
    private String tableTime;


    /**
     * 数据分析完成时间
     */
    private String analysisFinishTime;


    /**
     * 描述
     */
    private String nlpDesc;


    /**
     * 命中规则key为平台关键词、话术、情绪；value为 id-次数的集合
     *
     */
    private Map<String,List<WordFrequencyVO>> platHitRule;

    /**
     * 命中规则key为客户关键词、话术、情绪；value为 id-次数的集合
     */
    private Map<String, List<WordFrequencyVO>> customerHitRule;

    /**
     * 标签
     */
    private String tag;

    /**
     * 分数
     */
    @NotNull(message = "nlp处理分数不能为空")
    private String score;

    /**
     * 公钥
     */
    private String publicKeyStr;
}
