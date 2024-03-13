package com.ytl.vos.gateway.risk.facade.api;

import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.swagger.model.ResultData;

import com.ytl.vos.gateway.risk.service.AsrDataAnalysisService;
import com.ytl.vos.gateway.risk.service.DataService;
import com.ytl.vos.gateway.risk.utils.EncryptionUtils;
import com.ytl.vos.gateway.risk.vo.AsrProcessResultVO;
import com.ytl.vos.persistence.enums.SysParamEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author lingchuanyu
 * @date 2023/7/26-14:31
 */
@RestController
@RequestMapping("/api/asrDataAnalysis/")
@Slf4j
@Api(tags = "语音质检数据分析结果")
public class AsrDataAnalysisController {

    @Autowired
    AsrDataAnalysisService asrDataAnalysisService;

    @Autowired
    private DataService dataService;

    @ApiOperation("回调结果入库")
    @PostMapping("/addProcessResult")
    public ResultData addProcessResult(@Valid @RequestBody AsrProcessResultVO asrProcessResultVO) {
        String publicKeyStr = asrProcessResultVO.getPublicKeyStr();
        // 网关对外暴露接口 公私钥鉴权
        String privateKeyStr = dataService.getSysParam(SysParamEnum.ASR_PRIVATE_KEY);
        if (!checkToken(publicKeyStr, privateKeyStr)) {
            throw new BusinessException("未鉴权的请求");
        }
        asrDataAnalysisService.addProcessResult(asrProcessResultVO);
        return ResultData.SUCCESS;
    }


    /**
     * 校验公私钥
     *
     * @param publicKeyStr
     * @param privateKeyStr
     * @return
     */
    private boolean checkToken(String publicKeyStr, String privateKeyStr) {
        try {
            byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPublicKey);
            KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = publicKeyFactory.generatePublic(publicKeySpec);
            // 转换私钥字符串为私钥对象
            byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            KeyFactory privateKeyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = privateKeyFactory.generatePrivate(privateKeySpec);
            // 使用私钥进行签名
            byte[] signature = EncryptionUtils.sign(privateKey);
            // 使用公钥进行验证
            return EncryptionUtils.verify(signature, publicKey);
        } catch (Exception e) {
            log.error("公私钥验证失败");
            return false;
        }
    }


}
