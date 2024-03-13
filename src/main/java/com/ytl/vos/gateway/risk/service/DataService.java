package com.ytl.vos.gateway.risk.service;


import com.ytl.vos.persistence.dataservice.bo.*;
import com.ytl.vos.persistence.enums.SysParamEnum;

import java.util.List;

/**
 * 数据服务
 *
 * @author gan.feng
 */
public interface DataService {

    /**
     * 获取数据字典名称
     * @param codeType
     * @param codeId
     * @return
     */
    String getSysCodeName(String codeType, String codeId, String defName);

    /**
     * 获取数据字典名称
     * @param codeType
     * @param codeId
     * @param defName
     * @return
     */
    String getSysCodeName(String codeType, Byte codeId, String defName);



    /**
     * 获取数据字典
     * @param codeType
     * @param codeId
     * @return
     */
    SysDictCodeDataBO getSysCode(String codeType, String codeId);

    /**
     * 获取数据字典
     * @param codeType
     * @return
     */
    List<SysDictCodeDataBO> getSysCodeList(String codeType);


    /**
     * 系统参数
     *
     * @param sysParamEnum
     * @return
     */
    int getSysParamInt(SysParamEnum sysParamEnum);

    /**
     * 系统参数
     *
     * @param sysParamEnum
     * @return
     */
    String getSysParam(SysParamEnum sysParamEnum);

    /**
     * 获取平台关键词
     * @param ruleId
     * @return
     */
    RulePlatKeywordDataBO getPlatKeyword(String ruleId);

    /**
     * 获取平台关键词列表
     * @return
     */
    List<RulePlatKeywordDataBO> getPlatKeywordList();

    /**
     * 获取客户关键词
     * @param ruleId
     * @return
     */
    RuleCustomerKeywordDataBO getCustKeyword(String ruleId);

    /**
     * 获取客户关键词列表
     * @param userNo
     * @return
     */
    List<RuleCustomerKeywordDataBO> getCustKeywordList(String userNo);

    /**
     * 获取平台话术
     * @param ruleId
     * @return
     */
    RulePlatScripDataBO getPlatScrip(String ruleId);

    /**
     * 获取平台话术列表
     * @return
     */
    List<RulePlatScripDataBO> getPlatScripList();

    /**
     * 获取客户话术
     * @param ruleId
     * @return
     */
    RuleCustomerScripDataBO getCustScrip(String ruleId);

    /**
     * 获取客户话术列表
     * @param userNo
     * @return
     */
    List<RuleCustomerScripDataBO> getCustScripList(String userNo);

    /**
     * 获取ASR项目对象
     * @param projectNo 项目编号
     * @return
     */
    AsrProjectInfoDataBO getAsrProjectInfo(String projectNo);

    /**
     * 获取客户质检方案配置信息
     * @param userNo 客户账号
     * @return
     */
    String getPlanId(String userNo);

    /**
     * 获取客户质检方案配置信息
     * @param planId 方案ID
     * @return
     */
    RuleCustomerPlanConfigDataBO getPlanConfig(String planId);

    /**
     * 获取预警联系人信息
     * @param contactNo 联系人编号
     * @return
     */
    MonitorWarnContactInfoDataBO getWarnContact(String contactNo);

    /**
     * 获取平台消息通知配置
     * @param sysUserNo
     * @return
     */
    PlatNoteConfigDataBO getPlatNoteCfg(String sysUserNo);

    /**
     * 获取平台消息通知配置列表
     * @return
     */
    List<PlatNoteConfigDataBO> getPlatNoteCfgList();

    /**
     * 根据项目编号查询该项目的参数
     * @param projectNo 项目编号
     * @return
     */
    List<AsrProjectParamConfigDataBO> getAsrProjectParamByNo(String projectNo);

}
