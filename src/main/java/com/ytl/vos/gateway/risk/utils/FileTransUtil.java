package com.ytl.vos.gateway.risk.utils;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.vos.gateway.risk.service.bo.HttpSendResponseBO;
import com.ytl.vos.persistence.enums.AsrHandleStatusEnum;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileTransUtil {

    /**
     * 地域ID，常量内容
     */
    public static final String REGIONID = "cn-shenzhen";
    public static final String ENDPOINTNAME = "cn-shenzhen";
    public static final String PRODUCT = "nls-filetrans";
    public static final String DOMAIN = "filetrans.cn-shenzhen.aliyuncs.com";
    public static final String API_VERSION = "2018-08-17";
    public static final String POST_REQUEST_ACTION = "SubmitTask";
    public static final String GET_REQUEST_ACTION = "GetTaskResult";

    /**
     * 请求参数key
     */
    public static final String KEY_APP_KEY = "appkey";
    public static final String KEY_FILE_LINK = "file_link";
    public static final String KEY_VERSION = "version";
    /**
     * 是否开启词功能
     */
    public static final String KEY_ENABLE_WORDS = "enable_words";

    /**
     * 是否开启智能采样率
     */
    public static final String ENABLE_SAMPLE_RATE_ADAPTIVE ="enable_sample_rate_adaptive";
    /**
     * 是否启用回调功能，默认为false
     */
    public static final String ENABLE_CALLBACK = "enable_callback";
    public static final String CALLBACK_URL = "callback_url";
    /**
     * 是否开启智能分轨 非必填
     * （开启智能分轨，即可在两方对话的语音情景下，依据每句话识别结果中的ChannelId，判断该句话的发言人为哪一方。
     * 通常先发言一方ChannelId为0，8k双声道开启分轨后默认为2个人，声道channel0和channel1就是音轨编号）。
     */
    public static final String AUTO_SPLIT = "auto_split";

    /**
     * ITN（逆文本inverse text normalization）中文数字转换阿拉伯数字。设置为True时，中文数字将转为阿拉伯数字输出，默认值：False。
     */
    public static final String ENABLE_INVERSE_TEXT_NORMALIZATION = "enable_inverse_text_normalization";


    // 响应参数key
    public static final String KEY_TASK = "Task";
    /**
     * 识别任务ID。
     */
    public static final String KEY_TASK_ID = "TaskId";
    /**
     * 请求ID，用于调试。
     */
    public static final String KEY_REQUEST_ID = "RequestId";
    /**
     * 状态码
     */
    public static final String KEY_STATUS_CODE = "StatusCode";
    /**
     * 状态说明
     */
    public static final String KEY_STATUS_TEXT = "StatusText";

    /**
     * 时间戳（单位为毫秒），表示录音文件识别提交请求的时间
     */
    public static final String KEY_REQUEST_TIME = "RequestTime";
    /**
     * 时间戳（单位为毫秒），表示录音文件识别完成的时间
     */
    public static final String KEY_SOLVE_TIME = "SolveTime";

    /**
     *识别的音频文件总时长，单位为毫秒
     */
    public static final String KEY_BIZ_DURATION = "BizDuration";
    /**
     * 识别结果对象
     */
    public static final String KEY_RESULT = "Result";
    /**
     *识别的结果数据。当StatusText为SUCCEED时存在
     */
    public static final String KEY_SENTENCES = "Sentences";

    // 状态值
    public static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_QUEUEING = "QUEUEING";

    /**
     * 阿里云鉴权client
     */
    IAcsClient client;

    public FileTransUtil(String accessKeyId, String accessKeySecret){
        // 设置endpoint
        try {
            DefaultProfile.addEndpoint(ENDPOINTNAME, REGIONID, PRODUCT, DOMAIN);
        } catch (ClientException e) {
            e.printStackTrace();
        }
        // 创建DefaultAcsClient实例并初始化
        DefaultProfile profile = DefaultProfile.getProfile(REGIONID, accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    /**
     * 用于测试的方法
     * @param appKey
     * @param fileLink
     * @return
     */
    public String submitFileTransRequest(String appKey, String fileLink) {
        /**
         * 1. 创建CommonRequest 设置请求参数
         */
        CommonRequest postRequest = new CommonRequest();
        // 设置域名
        postRequest.setDomain(DOMAIN);
        // 设置API的版本号，格式为YYYY-MM-DD
        postRequest.setVersion(API_VERSION);
        // 设置action
        postRequest.setAction(POST_REQUEST_ACTION);
        // 设置产品名称
        postRequest.setProduct(PRODUCT);
        /**
         * 2. 设置录音文件识别请求参数，以JSON字符串的格式设置到请求的Body中
         */
        JSONObject taskObject = new JSONObject();
        // 设置appkey
        taskObject.put(KEY_APP_KEY, appKey);
        // 设置音频文件访问链接
        taskObject.put(KEY_FILE_LINK, fileLink);
        // 新接入请使用4.0版本，已接入(默认2.0)如需维持现状，请注释掉该参数设置
        taskObject.put(KEY_VERSION, "4.0");
        // 设置是否输出词信息，默认为false，开启时需要设置version为4.0及以上
        taskObject.put(KEY_ENABLE_WORDS, false);
        taskObject.put(AUTO_SPLIT,true);
        taskObject.put("enable_sample_rate_adaptive",true);
        String task = taskObject.toJSONString();
        System.out.println("[Ali ASR Record] requestBody:"+task);
        // 设置以上JSON字符串为Body参数
        postRequest.putBodyParameter(KEY_TASK, task);
        // 设置为POST方式的请求
        postRequest.setMethod(MethodType.POST);
        /**
         * 3. 提交录音文件识别请求，获取录音文件识别请求任务的ID，以供识别结果查询使用
         */
        String taskId = null;
        try {
            CommonResponse postResponse = client.getCommonResponse(postRequest);
            System.out.println("[Ali ASR Record] 提交录音文件识别请求的响应：" + postResponse.getData());
            if (postResponse.getHttpStatus() == 200) {
                JSONObject result = JSONObject.parseObject(postResponse.getData());
                String statusText = result.getString(KEY_STATUS_TEXT);
                if (STATUS_SUCCESS.equals(statusText)) {
                    taskId = result.getString(KEY_TASK_ID);
                }else {
                    //如果失败则将错误码和错误原因记录到数据库
                    String code = result.getString(KEY_STATUS_CODE);
                    System.out.println("[Ali ASR Record] 录音文件识别请求失败：{}"+result.toJSONString());
                    return "";
                }
            }else {
                System.out.println("[Ali ASR Record] 录音文件识别请求失败，Http错误码：{}"+postResponse.getHttpStatus());
                System.out.println("[Ali ASR Record] 录音文件识别请求失败响应：{}"+JSONObject.toJSONString(postResponse));
                return "";
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return taskId;
    }

    /**
     * 添加回调接口的用于项目的方法
     * @param appKey
     * @param fileLink
     * @param callbackUrl
     * @return
     */
    public HttpSendResponseBO submitFileTransRequest(String appKey, String fileLink,String callbackUrl,JSONObject paramObject) {
        HttpSendResponseBO responseBO = new HttpSendResponseBO();
        CommonRequest postRequest = new CommonRequest();
        // 设置域名
        postRequest.setDomain(DOMAIN);
        // 设置API的版本号，格式为YYYY-MM-DD
        postRequest.setVersion(API_VERSION);
        // 设置action
        postRequest.setAction(POST_REQUEST_ACTION);
        // 设置产品名称
        postRequest.setProduct(PRODUCT);
        JSONObject taskObject = new JSONObject();
        // 设置appkey
        taskObject.put(KEY_APP_KEY, appKey);
        // 设置音频文件访问链接
        taskObject.put(KEY_FILE_LINK, fileLink);
        // 新接入请使用4.0版本，已接入(默认2.0)如需维持现状，请注释掉该参数设置
        taskObject.put(KEY_VERSION, "4.0");
        // 设置是否输出词信息，默认为false
        taskObject.put(CALLBACK_URL,callbackUrl);
        taskObject.put(ENABLE_CALLBACK,true);
        taskObject.putAll(paramObject);
        //删除写死的参数，改成动态加载的方式
//        taskObject.put(AUTO_SPLIT,true);
//        taskObject.put(ENABLE_SAMPLE_RATE_ADAPTIVE,true);
//        taskObject.put(KEY_ENABLE_WORDS, false);
//        taskObject.put(ENABLE_INVERSE_TEXT_NORMALIZATION,true);
        String task = taskObject.toJSONString();
        log.info("[Ali ASR Record] requestBody:{}",task);
        // 设置以上JSON字符串为Body参数
        postRequest.putBodyParameter(KEY_TASK, task);
        // 设置为POST方式的请求
        postRequest.setMethod(MethodType.POST);
        String taskId = null;
        String requestId = null;
        try {
            CommonResponse postResponse = client.getCommonResponse(postRequest);
            log.info("[Ali ASR Record] 提交录音文件识别请求的响应：" + postResponse.getData());
            if (postResponse.getHttpStatus() == 200) {
                JSONObject result = JSONObject.parseObject(postResponse.getData());
                String statusText = result.getString(KEY_STATUS_TEXT);
                if (STATUS_SUCCESS.equals(statusText)) {
                    taskId = result.getString(KEY_TASK_ID);
                    responseBO.setTaskId(taskId);
                    requestId = result.getString(KEY_REQUEST_ID);
                    responseBO.setRequestId(requestId);
                    responseBO.setRespResult(AsrHandleStatusEnum.PROCESS);
                }else {
                    //如果失败则将错误码和错误原因记录到数据库
                    String code = result.getString(KEY_STATUS_CODE);
                    log.error("[Ali ASR Record] 录音文件识别请求失败：{}",result.toJSONString());
                    throw new BusinessException(code,statusText);
                }
            }else {
                log.error("[Ali ASR Record] 录音文件识别请求失败，Http错误码：{}",postResponse.getHttpStatus());
                log.error("[Ali ASR Record] 录音文件识别请求失败响应：{}",JSONObject.toJSONString(postResponse));
                throw new BusinessException(JSONObject.toJSONString(postResponse));
            }
        } catch (BusinessException e) {
            log.error("[Ali ASR Record] 提交录音文件识别失败",e);
            throw new BusinessException(e.getCode(),e.getMessage());
        }catch (Exception e) {
            log.error("[Ali ASR Record] 提交录音文件识别发生异常",e);
            throw new BusinessException(e.getMessage());
        }
        return responseBO;
    }






    /**
     * 使用轮询的方式查询转换结果，也可以配置回调
     * @param taskId 任务ID
     * @return
     */
    public String getFileTransResult(String taskId) {
        /**
         * 1. 创建CommonRequest 设置任务ID
         */
        CommonRequest getRequest = new CommonRequest();
        // 设置域名
        getRequest.setDomain(DOMAIN);
        // 设置API版本
        getRequest.setVersion(API_VERSION);
        // 设置action
        getRequest.setAction(GET_REQUEST_ACTION);
        // 设置产品名称
        getRequest.setProduct(PRODUCT);
        // 设置任务ID为查询参数
        getRequest.putQueryParameter(KEY_TASK_ID, taskId);
        // 设置为GET方式的请求
        getRequest.setMethod(MethodType.GET);
        /**
         * 2. 提交录音文件识别结果查询请求
         * 以轮询的方式进行识别结果的查询，直到服务端返回的状态描述为“SUCCESS”,或者为错误描述，则结束轮询。
         */
        String result = null;
        while (true) {
            try {
                CommonResponse getResponse = client.getCommonResponse(getRequest);
                System.err.println("识别查询结果：" + getResponse.getData());
                if (getResponse.getHttpStatus() != 200) {
                    System.err.println("识别结果查询请求失败，Http错误码： " + getResponse.getHttpStatus());
                    System.err.println("识别结果查询请求失败： " + getResponse.getData());
                    break;
                }
                JSONObject rootObj = JSONObject.parseObject(getResponse.getData());
                String statusText = rootObj.getString(KEY_STATUS_TEXT);
                if (STATUS_RUNNING.equals(statusText) || STATUS_QUEUEING.equals(statusText)) {
                    // 继续轮询，注意设置轮询时间间隔
                    Thread.sleep(3000);
                }
                else {
                    // 状态信息为成功，返回识别结果；状态信息为异常，返回空
                    if (STATUS_SUCCESS.equals(statusText)) {
                        result = rootObj.getString(KEY_RESULT);
                        // 状态信息为成功，但没有识别结果，则可能是由于文件里全是静音、噪音等导致识别为空
                        if(result == null) {
                            result = "";
                        }
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 3) {
            System.err.println("FileTransJavaDemo need params: <AccessKey Id> <AccessKey Secret> <app-key>");
        }
        final String accessKeyId = args[0];
        final String accessKeySecret = args[1];
        final String appKey = args[2];
//        String fileLink = "https://gw.alipayobjects.com/os/bmw-prod/0574ee2e-f494-45a5-820f-63aee583045a.wav";
//        String fileLink = "http://rtp.y5t.cn/669d23c0e234473092638c32914cb11f.mp3";
        String fileLink = "http://rtp.y5t.cn/2.mp3";
//        String fileLink = "http://rtp.y5t.cn/479d23c0e234473092638c32914cb11f.mp3";

//        String fileLink = "http://rtp.y5t.cn/5e7541f30c2c404e893dd274084aee58.mp3";
        FileTransUtil demo = new FileTransUtil(accessKeyId, accessKeySecret);
        // 第一步：提交录音文件识别请求，获取任务ID用于后续的识别结果轮询。
        String taskId = demo.submitFileTransRequest(appKey, fileLink);
        if (taskId != null) {
            System.out.println("录音文件识别请求成功，task_id: " + taskId);
        }
        else {
            System.out.println("录音文件识别请求失败！");
            return;
        }
        // 第二步：根据任务ID轮询识别结果。
        String result = demo.getFileTransResult(taskId);
        if (result != null) {
            System.out.println("录音文件识别结果查询成功：" + result);
        }
        else {
            System.out.println("录音文件识别结果查询失败！");
        }
    }
}
