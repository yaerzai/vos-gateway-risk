package com.ytl.vos.gateway.risk.service.impl;

import com.ytl.common.base.aop.MonitorSpendTimeAspect;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.service.bo.LocalCacheBO;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.vos.gateway.risk.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.persistence.dataservice.*;
import com.ytl.vos.persistence.dataservice.bo.*;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;


@Service
@Slf4j
public class DataServiceImpl implements DataService {

    @Resource
    private SysParamsDataService sysParamsDataService;
    @Resource
    private SysDictCodeDataService sysDictCodeDataService;
    @Resource
    private AsrProjectInfoDataService asrProjectInfoDataService;
    @Resource
    private RuleCustomerPlanConfigDataService ruleCustomerPlanConfigDataService;
    @Resource
    private RulePlatKeywordDataService rulePlatKeywordDataService;
    @Resource
    private RuleCustomerKeywordDataService ruleCustomerKeywordDataService;
    @Resource
    private RulePlatScripDataService rulePlatScripDataService;
    @Resource
    private RuleCustomerScripDataService ruleCustomerScripDataService;
    @Resource
    private MonitorWarnContactInfoDataService monitorWarnContactInfoDataService;
    @Resource
    private PlatNoteConfigDataService platNoteConfigDataService;
    @Resource
    private AsrProjectParamConfigDataService asrProjectParamConfigDataService;

    /**
     * 同步锁Map集合
     */
    private static Map<String, Lock> lockHashMap = new ConcurrentHashMap<>();
    /**
     * 通用缓存
     */
    private Map<String, Map<String, LocalCacheBO<Object>>> commonCacheMap = new ConcurrentHashMap<>();
    /**
     * 平台关键词本地缓存
     */
    private Map<String, LocalCacheBO<RuleCustomerKeywordDataBO>> ruleCustomerKeyWordMap = new ConcurrentHashMap<>();
    /**
     * 平台关键词本地缓存
     */
    private Map<String, LocalCacheBO<RulePlatKeywordDataBO>> rulePlatKeyWordMap = new ConcurrentHashMap<>();

    public static DataService instance;

    @PostConstruct
    private void init() {
        instance = this;
    }

    private Object getCacheBO(String commonCacheKey, String cacheName, Supplier<Object> newCacheBO, BiFunction<Long, Long, Boolean> compare) {
        Map<String, LocalCacheBO<Object>> cacheMap = commonCacheMap.computeIfAbsent(commonCacheKey, key -> new ConcurrentHashMap<>());
        LocalCacheBO<Object> localCacheBO = cacheMap.computeIfAbsent(cacheName, key -> new LocalCacheBO(newCacheBO.get(), new AtomicLong((System.currentTimeMillis()))));
        synchronized (localCacheBO) {
            if (!compare.apply(System.currentTimeMillis(), localCacheBO.getLastLoadTime().get())) {
                localCacheBO.setDataBO(newCacheBO.get());
                localCacheBO.getLastLoadTime().getAndSet(System.currentTimeMillis());
                log.info("[二级缓存] 重载信息 {} {}", commonCacheKey, cacheName);
            }
            return localCacheBO.getDataBO();
        }
    }

    @Override
    public String getSysCodeName(String codeType, Byte codeId, String defName) {
        if (codeId == null) {
            return defName;
        }
        return getSysCodeName(codeType, codeId.toString(), defName);
    }

    @Override
    public String getSysCodeName(String codeType, String codeId, String defName) {
        return getSysCodeList(codeType).stream().filter(item -> item.getCodeId().equals(codeId)).map(SysDictCodeDataBO::getCodeName).findAny().orElse(defName);
    }

    @Override
    public SysDictCodeDataBO getSysCode(String codeType, String codeId) {
        return getSysCodeList(codeType).stream().filter(item -> item.getCodeId().equals(codeId)).findAny().orElse(null);
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("SysCodeList")
    public List<SysDictCodeDataBO> getSysCodeList(String codeType) {
        return (List<SysDictCodeDataBO>) getCacheBO("SysCodeList", codeType,
                () -> sysDictCodeDataService.queryList(codeType),
                DateUtils::isSameMinute
        );
    }


    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("SysParamInt")
    public int getSysParamInt(SysParamEnum sysParamEnum) {
        String paramValue = getSysParam(sysParamEnum);
        try {
            return Integer.valueOf(paramValue);
        } catch (NumberFormatException e) {
            log.error("系统参数配置错误,非法数字 {}", sysParamEnum);
            throw new BusinessException(VosErrCodeEnum.System_Param_Config_Error, "系统参数配置错误,非法数字");
        }
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("SysParam")
    public String getSysParam(SysParamEnum sysParamEnum) {
        String paramName = sysParamEnum.getParamName();
        return (String) getCacheBO("SysParam", paramName,
                ()-> sysParamsDataService.get(paramName, sysParamEnum.getDefaultValue()),
                DateUtils::isSameMinute
        );
    }

    /**
     * 获取平台关键词
     *
     * @param ruleId
     * @return
     */
    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatKeyword")
    public RulePlatKeywordDataBO getPlatKeyword(String ruleId) {
        return (RulePlatKeywordDataBO) getCacheBO("PlatKeyword", ruleId,
                ()-> rulePlatKeywordDataService.getCache("ALL", ruleId),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatKeywordList")
    public List<RulePlatKeywordDataBO> getPlatKeywordList() {
        return (List<RulePlatKeywordDataBO>) getCacheBO("PlatKeywordList", "ALL",
                ()-> rulePlatKeywordDataService.getCacheList("ALL"),
                DateUtils::isSameMinute
        );
    }

    /**
     * 获取客户关键词
     *
     * @param ruleId
     * @return
     */
    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("CustKeyword")
    public RuleCustomerKeywordDataBO getCustKeyword(String ruleId) {
        return (RuleCustomerKeywordDataBO) getCacheBO("CustKeyword", ruleId,
                ()-> ruleCustomerKeywordDataService.getCache("ALL", ruleId),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("CustKeywordList")
    public List<RuleCustomerKeywordDataBO> getCustKeywordList(String userNo) {
        return (List<RuleCustomerKeywordDataBO>) getCacheBO("CustKeywordList", userNo,
                ()-> ruleCustomerKeywordDataService.getCacheList(userNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatScrip")
    public RulePlatScripDataBO getPlatScrip(String ruleId) {
        return (RulePlatScripDataBO) getCacheBO("PlatScrip", ruleId,
                ()-> rulePlatScripDataService.getCache("ALL", ruleId),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatScripList")
    public List<RulePlatScripDataBO> getPlatScripList() {
        return (List<RulePlatScripDataBO>) getCacheBO("PlatScripList", "ALL",
                ()-> rulePlatScripDataService.getCacheList("ALL"),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("CustScrip")
    public RuleCustomerScripDataBO getCustScrip(String ruleId) {
        return (RuleCustomerScripDataBO) getCacheBO("CustScrip", ruleId,
                ()-> rulePlatScripDataService.getCache("ALL", ruleId),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("CustScripList")
    public List<RuleCustomerScripDataBO> getCustScripList(String userNo) {
        return (List<RuleCustomerScripDataBO>) getCacheBO("CustScripList", userNo,
                ()-> ruleCustomerScripDataService.getCacheList(userNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("AsrProject")
    public AsrProjectInfoDataBO getAsrProjectInfo(String projectNo) {
        return (AsrProjectInfoDataBO) getCacheBO("AsrProject", projectNo,
                () -> asrProjectInfoDataService.getDateByProjectNO(projectNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlanId")
    public String getPlanId(String userNo) {
        return (String) getCacheBO("PlanId", userNo,
                () -> ruleCustomerPlanConfigDataService.getCachePlanIdByUser(userNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlanConfig")
    public RuleCustomerPlanConfigDataBO getPlanConfig(String planId) {
        return (RuleCustomerPlanConfigDataBO) getCacheBO("PlanConfig", planId,
                () -> ruleCustomerPlanConfigDataService.getCache("ALL", planId),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("WarnContact")
    public MonitorWarnContactInfoDataBO getWarnContact(String contactNo) {
        return (MonitorWarnContactInfoDataBO) getCacheBO("MonitorWarnContact", contactNo,
                () -> monitorWarnContactInfoDataService.get(contactNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatNoteCfg")
    public PlatNoteConfigDataBO getPlatNoteCfg(String sysUserNo) {
        return (PlatNoteConfigDataBO) getCacheBO("PlatNoteCfg", sysUserNo,
                ()-> platNoteConfigDataService.getCache("ALL", sysUserNo),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("PlatNoteCfgList")
    public List<PlatNoteConfigDataBO> getPlatNoteCfgList() {
        return (List<PlatNoteConfigDataBO>) getCacheBO("PlatNoteCfgList", "ALL",
                ()-> platNoteConfigDataService.getCacheList("ALL"),
                DateUtils::isSameMinute
        );
    }

    @Override
    public List<AsrProjectParamConfigDataBO> getAsrProjectParamByNo(String projectNo) {
        return (List<AsrProjectParamConfigDataBO>) getCacheBO("AsrProjectParam", projectNo,
                ()-> asrProjectParamConfigDataService.query(AsrProjectParamConfigQueryBO.builder().projectNo(projectNo).build()).getData(),
                DateUtils::isSameMinute
        );
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void clearLocalCache() {
        log.warn("[ClearLocalCache] 执行");
        int betweenTime = getSysParamInt(SysParamEnum.LOCAL_CACHE_TIME_OUT) * 60 * 1000;
        List<Map<String, LocalCacheBO<Object>>> commonCacheList= new ArrayList<>(commonCacheMap.values());
        for (Map<String, LocalCacheBO<Object>> cacheMap : commonCacheList) {
            List<String> cacheKeyList = new ArrayList<>(cacheMap.keySet());
            cacheKeyList.forEach(cacheKey-> {
                LocalCacheBO<Object> cacheObj = cacheMap.get(cacheKey);
                if (DateUtils.getCurrentTimeMillis() - cacheObj.getLastLoadTime().get() >= betweenTime) {
                    log.warn("[ChannelInfoMap] remove local cache key:{}", cacheKey);
                    cacheMap.remove(cacheKey);
                }
            });
        }
    }

    /**
     * 同步锁
     *
     * @param key
     * @return
     */
    private synchronized Lock getSyncLock(String key) {
        Lock lock = lockHashMap.get(key);
        if (lock == null) {
            lock = new ReentrantLock();
            lockHashMap.put(key, lock);
        }
        return lock;
    }
}
