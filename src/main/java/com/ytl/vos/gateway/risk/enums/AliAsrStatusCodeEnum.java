package com.ytl.vos.gateway.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author kf-zhanghui
 * @date 2023/7/27 18:00
 */
@Getter
@AllArgsConstructor
public enum AliAsrStatusCodeEnum {

    /**
     *
     */
    SUCCESS("21050000","成功"),
    RUNNING("21050001","录音文件识别任务运行中"),
    QUEUEING("21050002","录音文件识别任务排队中"),
    SUCCESS_WITH_NO_VALID_FRAGMENT("21050003","识别结果查询接口调用成功，但是没有识别到有效语音"),

    /**
     * 免费用户每日可识别不超过2小时时长的录音文件
     */
    USER_BIZDURATION_QUOTA_EXCEED("41050001","单日时间超限"),

    /**
     * 检查录音文件路径是否正确，是否可以外网访问和下载
     */
    FILE_DOWNLOAD_FAILED("41050002","文件下载失败"),

    /**
     * 检查录音文件是否是单轨/双轨的WAV格式或MP3格式
     */
    FILE_CHECK_FAILED("41050003","文件格式错误"),

    /**
     * 检查录音文件大小是否超过512 MB，超过则需您对录音文件分段
     */
    FILE_TOO_LARGE("41050004","文件过大"),

    FILE_NORMALIZE_FAILED("41050005","文件归一化失败,检查录音文件是否有损坏，是否可以正常播放"),

    FILE_PARSE_FAILED("41050006","文件解析失败,检查录音文件是否有损坏，是否可以正常播放"),

    MKV_PARSE_FAILED("41050007","MKV解析失败,检查录音文件是否有损坏，是否可以正常播放"),

    /**
     * 检查实际语音的采样率和控制台上Appkey绑定的ASR模型采样率是否一致，
     * 或者将本篇文档中自动降采样的参数enable_sample_rate_adaptive设置为true
     */
    UNSUPPORTED_SAMPLE_RATE("41050008","采样率不匹配"),

    /**
     * TaskId不存在，或者已过期
     */
    FILE_TRANS_TASK_EXPIRED("41050010","录音文件识别任务过期"),

    /**
     * 确认file_link参数格式是否正确
     */
    REQUEST_INVALID_FILE_URL_VALUE("41050011","请求file_link参数非法"),
    /**
     * 确认callback_url参数格式是否正确，是否为空
     */
    REQUEST_INVALID_CALLBACK_VALUE("41050012","请求callback_url参数非法"),

    /**
     * 确认请求task值为有效JSON格式字符串
     */
    REQUEST_PARAMETER_INVALID("41050013","请求参数无效"),

    REQUEST_EMPTY_APPKEY_VALUE("41050014","请求参数appkey值为空"),

    /**
     * 确认请求参数appkey值是否设置正确，或者是否与阿里云账号的AccessKey ID同一个账号
     */
    REQUEST_APPKEY_UNREGISTERED("41050015","请求参数appkey未注册"),

    /**
     * 检查您的RAM用户是否已经授权调用语音服务的API
     */
    RAM_CHECK_FAILED("41050021","RAM检查失败"),

    CONTENT_LENGTH_CHECK_FAILED("41050023","content-length 检查失败"),

    FILE_404_NOT_FOUND("41050024","需要下载的文件不存在"),

    FILE_403_FORBIDDEN("41050025","没有权限下载需要的文件"),

    FILE_SERVER_ERROR("41050026","请求的文件所在的服务不可用"),

    ASR_RESPONSE_HAVE_HO_WORDS("41050029","未识别出有效文字"),

    /**
     * 确认文件下载链接中文件为支持的音频格式
     */
    DECODER_ERROR("40270003","检测音频文件信息失败"),
    ;
    /**
     * 状态码
     */
    private final String code;

    /**
     * 描述
     */
    private final String msg;


    /**
     * 解析
     * @param code 状态码
     * @return
     */
    public static AliAsrStatusCodeEnum parse(String code) {
        return Arrays.stream(values()).filter(item-> item.code.equals(code)).findAny().orElse(null);
    }
}
