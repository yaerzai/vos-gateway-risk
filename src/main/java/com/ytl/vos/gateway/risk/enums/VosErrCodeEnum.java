package com.ytl.vos.gateway.risk.enums;

import com.ytl.common.base.exception.ServerCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author
 */
@Getter
@AllArgsConstructor
public enum VosErrCodeEnum implements ServerCode {

    /**
     * 错误码
     */
    System_Maintain("SY.0000", "SY.0000", "系统维护"),
    System_Error("SY.9999", "SY.9999", "系统未知错误"),

    Param_Validate_Error("PA.0001", "PA.0001", "参数格式校验错误"),
    Param_Invalid_Retrun("PA.0002", "PA.0002", "无效返回值"),
    System_Param_Config_Error("PA.0003", "PA.0003", "系统参数配置异常"),

    Customer_NotExists("CS.0001", "CS.0001", "客户不存在"),
    Customer_Stoped("CS.0002", "CS.0002", "客户已停用"),
    Customer_Balance_Less("CS.0003", "CS.0003", "客户余额不足"),
    Customer_User_NotExists("CS.0004", "CS.0004", "客户账号不存在"),
    Customer_User_Stoped("CS.0005", "CS.0005", "客户账号已停用"),
    Customer_MasterUser_NotExists("CS.0006", "CS.0006", "客户主账号不存在"),
    Customer_IpCheck_Error("CS.0007", "CS.0007", "IP地址不合法"),
    Customer_OutFlow("CS.0008", "CS.0008", "账号请求超流速"),
    Customer_OutDayLimit("CS.0009", "CS.0009", "超过账号日限"),
    Customer_OutMonthLimit("CS.0010", "CS.0010", "超过账号月限"),
    Customer_Fee_UnBind("CS.0011", "CS.0011", "未绑定费率"),
    Customer_Number_OutMaxConc("CS.0012", "CS.0012", "超过号码最大并发"),

    Number_Unsub_Black("MO.0001", "MO.0001", "退订黑名单"),
    Number_Complaint_Black("MO.0002", "MO.0002", "投诉黑名单"),
    Number_Third_Black("MO.0003", "MO.0003", "第三方黑名单"),
    Number_Not_Available("MO.0004", "MO.0004", "无可用号码"),
    Number_Province_No_PublicNumber("MO.0005", "MO.0005", "省份无公共号码"),
    Number_NotEnable("MO.0006", "MO.0006", "号码非启用状态"),

    DB_Insert_Error("DB.0001", "DB.0001", "数据库插入异常"),

    Channel_No_Use("CH.0001", "CH.0001", "无可用通道"),
    Channel_Disable("CH.0002", "CH.0002", "通道已停用"),
    Channel_No_PublicNumber("CH.0003", "CH.0003", "通道无公共号码"),
    Channel_Invalid("CH.0004", "CH.0004", "无效通道"),


    ASR_ACCESS_KEY_ID_BLANK("ASR.0001","ASR.0001","ASR账号ID为空"),
    ASR_ACCESS_KEY_SECRET_BLANK("ASR.0002","ASR.0002","ASR账号密码为空"),
    ASR_APP_KEY_BLANK("ASR.0003","ASR.0003","ASR应用Key为空"),
    ASR_ALI_CALLBACK_URL_BLANK("ASR.0004","ASR.0004","阿里ASR回调地址为空"),
    ASR_VOICE_URL_BLANK("ASR.0005","ASR.0005","录音文件下载地址为空"),

    SMS_SERVER_REQUEST_ERROR("ASR.7012", "ASR.7012","短信服务请求失败"),
    SMS_SECRET_ERROR("ASR.7013", "ASR.7013","配置的预警短信服务的账号和私钥有误"),
    CALL_LOG_BLANK("ASR.7014", "ASR.7014","未找到相应的话单记录"),
    ASR_PROCESS_BLANK("ASR.7015", "ASR.7015","未找到相应的处理结果信息"),

    ;

    /**
     * 操作代码
     */
    private final String code;

    /**
     * 对外错误码
     */
    private final String outCode;

    /**
     * 描述
     */
    private final String msg;

    /**
     * 比较
     * @param code
     * @return
     */
    private boolean eq(String code) {
        return this.code.equals(code);
    }

    /**
     * 解析
     * @param code
     * @return
     */
    public static VosErrCodeEnum parse(String code) {
        return Arrays.stream(values()).filter(item-> item.code.equals(code)).findAny().orElse(null);
    }

}
