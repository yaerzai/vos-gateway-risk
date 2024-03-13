package com.ytl.vos.gateway.risk.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HttpUtils {

    private static final int RETRY_COUNT = 3;
    public static String postJson(String httpUrl, String reqBody) {
        log.info("请求地址: {}", httpUrl);
        log.info("请求参数: {}", reqBody);

        for (int i = 0; i < RETRY_COUNT; i++) { //发送失败重试
            try {
                HttpRequest request = HttpUtil.createPost(httpUrl);
                request.body(reqBody);
                HttpResponse response = request.execute();
                if (!response.isOk()) {
                    log.error("响应失败: {}", response.getStatus());
                    return null;
                }
                String respBody = response.body();
                if (respBody == null) {
                    log.error("响应空内容");
                    return null;
                }
                log.debug("响应内容: {}", respBody);
                return respBody;
            } catch (RuntimeException e) {
                if (e.getMessage().contains("签名校验")) {
                    throw e;
                }
                log.error("第{}次请求发生异常: {}", i+1, e.getMessage());
                if (i == RETRY_COUNT -1) {
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * 在指定url后追加参数
     * @param url 原始地址
     * @param data 添加的参数集合
     * @return
     */
    public static String appendUrl(String url, Map<String,Object> data) {
        String newUrl = url;
        StringBuffer param = new StringBuffer();
        for (String key : data.keySet()) {
            param.append(key + "=" + data.get(key).toString() + "&");
        }
        String paramStr = param.toString();
        paramStr = paramStr.substring(0, paramStr.length() - 1);
        if (newUrl.indexOf("?") >= 0) {
            newUrl += "&" + paramStr;
        } else {
            newUrl += "?" + paramStr;
        }
        return newUrl;
    }

    /**
     * 获取指定url中的某个参数
     * @param url
     * @param name
     * @return
     */
    public static String getParamByUrl(String url, String name) {
        url += "&";
        String pattern = "(\\?|&){1}#{0,1}" + name + "=[a-zA-Z0-9]*(&{1})";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(url);
        if (m.find( )) {
            return m.group(0).split("=")[1].replace("&", "");
        } else {
            return null;
        }
    }

}
