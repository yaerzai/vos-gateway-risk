package com.ytl.vos.gateway.risk.facade.api;

import com.ytl.common.swagger.model.ResultData;
import com.ytl.vos.gateway.risk.service.AsrRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author kf-zhanghui
 */
@RequestMapping("/asrFileTrans/callback")
@RestController
@Slf4j
public class FileTransCallBackController {

    @Resource(name = "aliYunRecognizeService")
    private AsrRecognitionService asrRecognitionService;

    /**
     * 必须是post方式
     * @param body
     * @return
     */
    @RequestMapping(value = "result", method = RequestMethod.POST)
    public ResultData getResult(@RequestBody String body) {
        asrRecognitionService.callback(body);
        return ResultData.SUCCESS;
    }
}