package com.ytl.vos.gateway.risk;

import com.alibaba.fastjson.JSONArray;
import com.ytl.vos.gateway.risk.service.NlpService;
import com.ytl.vos.gateway.risk.utils.FileTransUtil;
import com.ytl.vos.gateway.risk.utils.KeywordUtils;
import com.ytl.vos.jms.code.dto.asr.AsrAliYunDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

/**
 * @author kf-zhanghui
 * @date 2023/8/2 9:23
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class NlpServiceImplTest {

    @Resource
    private NlpService nlpService;

    @Test
    public void test01() {
        String taskId = "";

        String result = "{\"Sentences\":[{\"EndTime\":1020,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":210,\"Text\":\"喂。\",\"ChannelId\":1,\"SpeechRate\":74,\"EmotionValue\":6.7},{\"EndTime\":3040,\"SilenceDuration\":2,\"SpeakerId\":\"1\",\"BeginTime\":2240,\"Text\":\"喂。\",\"ChannelId\":0,\"SpeechRate\":75,\"EmotionValue\":5.5},{\"EndTime\":11041,\"SilenceDuration\":1,\"SpeakerId\":\"2\",\"BeginTime\":2370,\"Text\":\"哎，您好，我是洋钱罐客户经理，之前有联系过您的系统呢，给您预审批了一笔最高十六万八八的资金。\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":5.5},{\"EndTime\":15905,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":11041,\"Text\":\"您还没有领取这笔资金呢，给到您的借款利息，非常划算。\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":4.6},{\"EndTime\":15400,\"SilenceDuration\":11,\"SpeakerId\":\"1\",\"BeginTime\":14550,\"Text\":\"那个。\",\"ChannelId\":0,\"SpeechRate\":141,\"EmotionValue\":4.6},{\"EndTime\":22673,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":15905,\"Text\":\"一万块钱每天的利息最低就只需要两块多，可以随借随还用一天算一天额度。\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":6.8},{\"EndTime\":26903,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":22673,\"Text\":\"领取成功之后呢，不使用是不会产生任何费用的。\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":6.8},{\"EndTime\":31133,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":26903,\"Text\":\"今天关注我们洋钱罐借款小程序就可以领取到。\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":6.8},{\"EndTime\":33460,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":31133,\"Text\":\"我先带您看一下，您看好吗？\",\"ChannelId\":1,\"SpeechRate\":283,\"EmotionValue\":6.8},{\"EndTime\":35130,\"SilenceDuration\":18,\"SpeakerId\":\"1\",\"BeginTime\":33640,\"Text\":\"可以，这怎么弄呀？\",\"ChannelId\":0,\"SpeechRate\":281,\"EmotionValue\":5.6},{\"EndTime\":39007,\"SilenceDuration\":2,\"SpeakerId\":\"2\",\"BeginTime\":35760,\"Text\":\"您不要误会哈，不是让您马上来借钱。\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":6.8},{\"EndTime\":49401,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":39007,\"Text\":\"主要呢今天咱们在洋钱罐借款小程序给像您一样的优质客户准备了绿色审核通道，通过率和额度要比平时高很多。\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":5.9},{\"EndTime\":48450,\"SilenceDuration\":11,\"SpeakerId\":\"1\",\"BeginTime\":46810,\"Text\":\"我我知道我知道。\",\"ChannelId\":0,\"SpeechRate\":256,\"EmotionValue\":5.9},{\"EndTime\":55030,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":49401,\"Text\":\"建议您趁着今天额度高，下款快，赶紧到洋钱罐小程序登录领取。\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":6.8},{\"EndTime\":62392,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":55030,\"Text\":\"今天呢额外还能有最高三十天的免息券，就相当于可以免费使用借款一段时间了。\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":6.8},{\"EndTime\":69538,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":62392,\"Text\":\"目前呢我们特邀请的其他客户都完成领取了，给到您这边的优惠也快要过期了。\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":4.5},{\"EndTime\":64500,\"SilenceDuration\":15,\"SpeakerId\":\"1\",\"BeginTime\":63780,\"Text\":\"嗯嗯。\",\"ChannelId\":0,\"SpeechRate\":166,\"EmotionValue\":4.5},{\"EndTime\":71920,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":69538,\"Text\":\"您今天就不要错过了，好吗？\",\"ChannelId\":1,\"SpeechRate\":277,\"EmotionValue\":6.8},{\"EndTime\":75060,\"SilenceDuration\":7,\"SpeakerId\":\"1\",\"BeginTime\":72150,\"Text\":\"好的好的，你跟我说怎么弄，我领取一下。\",\"ChannelId\":0,\"SpeechRate\":329,\"EmotionValue\":5.7},{\"EndTime\":85001,\"SilenceDuration\":3,\"SpeakerId\":\"2\",\"BeginTime\":75390,\"Text\":\"嗯，那您打开微信，点击微信上方的放大镜，直接搜索杨钱罐借款这五个字，再给您重复一下哈。\",\"ChannelId\":1,\"SpeechRate\":237,\"EmotionValue\":6.8},{\"EndTime\":89806,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":85001,\"Text\":\"洋是海洋的，洋钱是有钱的，钱罐是易拉罐的罐。\",\"ChannelId\":1,\"SpeechRate\":237,\"EmotionValue\":5.7},{\"EndTime\":89550,\"SilenceDuration\":12,\"SpeakerId\":\"1\",\"BeginTime\":87950,\"Text\":\"哪个杨哪个杨？\",\"ChannelId\":0,\"SpeechRate\":225,\"EmotionValue\":5.7},{\"EndTime\":91830,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":89806,\"Text\":\"洋钱罐借款五个字。\",\"ChannelId\":1,\"SpeechRate\":237,\"EmotionValue\":6.8},{\"EndTime\":93140,\"SilenceDuration\":2,\"SpeakerId\":\"1\",\"BeginTime\":92040,\"Text\":\"哪个杨。\",\"ChannelId\":0,\"SpeechRate\":163,\"EmotionValue\":6.0},{\"EndTime\":97539,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":92110,\"Text\":\"点击一下小程序选项，直接关注小程序的第一个洋钱罐借款。\",\"ChannelId\":1,\"SpeechRate\":276,\"EmotionValue\":6.0},{\"EndTime\":95320,\"SilenceDuration\":1,\"SpeakerId\":\"1\",\"BeginTime\":94160,\"Text\":\"哪个杨。\",\"ChannelId\":0,\"SpeechRate\":155,\"EmotionValue\":6.0},{\"EndTime\":101380,\"SilenceDuration\":0,\"SpeakerId\":\"1\",\"BeginTime\":95770,\"Text\":\"妈了个逼，我问你哪个杨，我操你妈逼我问你哪个杨。\",\"ChannelId\":0,\"SpeechRate\":224,\"EmotionValue\":6.6},{\"EndTime\":99060,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":97539,\"Text\":\"您先看一下好吗？\",\"ChannelId\":1,\"SpeechRate\":276,\"EmotionValue\":6.6},{\"EndTime\":104200,\"SilenceDuration\":0,\"SpeakerId\":\"2\",\"BeginTime\":99870,\"Text\":\"好吧，那不好意思打扰您了，祝您生活愉快，再见。\",\"ChannelId\":1,\"SpeechRate\":263,\"EmotionValue\":6.6},{\"EndTime\":104200,\"SilenceDuration\":1,\"SpeakerId\":\"1\",\"BeginTime\":102450,\"Text\":\"打扰你妈了个逼。\",\"ChannelId\":0,\"SpeechRate\":240,\"EmotionValue\":6.7}]}";
//        nlpService.nlpAnalyze();

    }


    @Test
    public void test02() {
        String text = "喂。|喂。|喂，您好，我是泰康医疗保障的。|我们这边医疗系统呢查询到您有一份保额十万元的重疾险，怎么没有领取啊，就是其他客户都已经全部领领取成功了。|这个免费的重疾险保障时间是一年，短信稍后后再一次发送给您，您务必领取一下，好吧。|嗯，我不知道怎么领取呀。|那稍后我会把专属链接短信发给您，您通过短信打开链接就可以查看领取保障了。|如果觉得不错，还可以升级保障给您的保额也会更高。|因为最主要呢目前是特殊时期嘛，我们呢还可以给大家把保障升级到六百万，其他客户呢都可以参与进来了。|升级之后就是不管大病小病都能保，像小到感冒发烧，大到癌症肿瘤，所有的医药费呢都可以百分之百的报销。|这个合同条款里面都会有的，您请放心。|稍后啊您收到短信了，不要错过这次机会了，好吧。|嗯，好的。|嗯，那领取保障之后呢，如果说发生风险，咱们就不用担心花自己辛苦挣的血汗钱了嘛。|而且呢这次保费非常优惠，最低只要一块钱就可以升级，而且全家老小都可以保进来。|活动即将结束，您收到短信之后呢，务必尽快操作领取，那感谢您的支持，再见。";
        String[] keywords = Arrays.copyOf(new String[]{"您好"},1);
        Map<String, Map<String, Integer>> stringMapMap = KeywordUtils.synonymKeywords(text, keywords);
        System.out.println(stringMapMap);

    }



}
