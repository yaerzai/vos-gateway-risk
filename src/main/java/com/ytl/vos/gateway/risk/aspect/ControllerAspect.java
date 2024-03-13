package com.ytl.vos.gateway.risk.aspect;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytl.common.base.controller.SystemController;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.vos.gateway.risk.enums.VosErrCodeEnum;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
@Order(1)
public class ControllerAspect {

    public static Map<String, Class> dtoClassMap = new ConcurrentHashMap<>();

    @Pointcut("execution(* com.ytl.vos.gateway.risk.controller..*(..))")
    public void apiItf() {
    }

    @Before("apiItf()")
    public void before(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;
        showReqParams(joinPoint, args);
        if (!SystemController.getSystemOpenFlag()) {
            throw new BusinessException(VosErrCodeEnum.System_Maintain);
        }
    }


    @AfterReturning(returning = "returnObj", value = "apiItf()")
    public Object afterReturning(JoinPoint joinPoint, Object returnObj) {
        if (ObjectUtil.isNull(returnObj)) {
            throw new BusinessException(VosErrCodeEnum.Param_Invalid_Retrun, "返回数数据异常null");
        }
        showResParams(returnObj);
        return returnObj;
    }

    // 显示输入参数
    private void showReqParams(JoinPoint joinPoint, Object[] args) {
        Method proxyMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = proxyMethod.getName();
        ApiOperation apiOperation = proxyMethod.getDeclaredAnnotation(ApiOperation.class);
        String showName = apiOperation == null ? methodName : apiOperation.value();// + "-请求";
        Object printArgs = args;
        if (args.length > 0) {
            Class<?> voClass = args[0].getClass();
            String name = voClass.getSimpleName();
            String voName = StrUtil.lowerFirst(name);
            if (!dtoClassMap.containsKey(voName)) {
                dtoClassMap.put(voName, voClass);
            }
            printArgs = JSONUtil.toJsonStr(args[0]);
        }
        MDC.put("type", showName);
        log.info("[请求] {}", printArgs);
    }

    // 显示输出参数
    private void showResParams(Object returnObj) {
        String paramsStr = JSONUtil.toJsonStr(returnObj);
        log.info("[响应] {}", paramsStr);
    }

}
