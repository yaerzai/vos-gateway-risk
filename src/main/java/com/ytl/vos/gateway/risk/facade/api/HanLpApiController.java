package com.ytl.vos.gateway.risk.facade.api;

import com.ytl.common.swagger.model.ResultData;
import com.ytl.vos.gateway.risk.utils.KeywordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * @author kf-zhanghui
 * @date 2023/8/9 18:39
 */

@RequestMapping("/hanlp/api")
@RestController
@Slf4j
public class HanLpApiController {


    @PostMapping("/test")
    public ResultData synonymKeywords() {
        String text = "喂。|喂。|喂，您好，我是泰康医疗保障的。|我们这边医疗系统呢查询到您有一份保额十万元的重疾险，怎么没有领取啊，就是其他客户都已经全部领领取成功了。|这个免费的重疾险保障时间是一年，短信稍后后再一次发送给您，您务必领取一下，好吧。|嗯，我不知道怎么领取呀。|那稍后我会把专属链接短信发给您，您通过短信打开链接就可以查看领取保障了。|如果觉得不错，还可以升级保障给您的保额也会更高。|因为最主要呢目前是特殊时期嘛，我们呢还可以给大家把保障升级到六百万，其他客户呢都可以参与进来了。|升级之后就是不管大病小病都能保，像小到感冒发烧，大到癌症肿瘤，所有的医药费呢都可以百分之百的报销。|这个合同条款里面都会有的，您请放心。|稍后啊您收到短信了，不要错过这次机会了，好吧。|嗯，好的。|嗯，那领取保障之后呢，如果说发生风险，咱们就不用担心花自己辛苦挣的血汗钱了嘛。|而且呢这次保费非常优惠，最低只要一块钱就可以升级，而且全家老小都可以保进来。|活动即将结束，您收到短信之后呢，务必尽快操作领取，那感谢您的支持，再见。";
        String[] keywords = Arrays.copyOf(new String[]{"您好"},1);
        Map<String, Map<String, Integer>> stringMapMap = KeywordUtils.synonymKeywords(text, keywords);
        log.info("[synonymKeywords] result:{}",stringMapMap.toString());
        return ResultData.getSuccessData(stringMapMap);
    }
}
