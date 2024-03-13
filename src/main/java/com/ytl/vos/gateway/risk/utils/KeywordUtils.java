package com.ytl.vos.gateway.risk.utils;

import cn.hutool.core.util.StrUtil;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.synonym.Synonym;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;
import com.hankcs.hanlp.dictionary.common.CommonSynonymDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.ytl.vos.gateway.risk.service.bo.KeyWord;
import com.ytl.vos.persistence.enums.CheckRangeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kf-zhanghui
 * @date 2023/7/29 11:14
 * 关键词处理工具
 */
@Slf4j
public class KeywordUtils {

    private static final String PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    /**
     * 关键词完全匹配
     * @param text 文本
     * @param keywords 关键词列表
     * @return
     */
    public static Map<String, Integer> perfectMatchKeywords(String text, String[] keywords) {
        Map<String, Integer> resultMap = new HashMap<>();
        if(StrUtil.isEmpty(text) || keywords.length <=0) {
            return resultMap;
        }
        for (String keyword : keywords) {
            if(StrUtil.isEmpty(keyword)){
                continue;
            }
            Integer integer = keywordTimeCount(text, keyword);
            if(integer > 0){
                resultMap.put(keyword, integer);
            }
        }
        return resultMap;
    }


    /**
     * 近似词匹配
     * @param text 文本
     * @param keywords 关键词列表
     * @return
     */
    public static Map<String, Map<String,Integer>> synonymKeywords(String text, String[] keywords) {


        Map<String, Map<String,Integer>> resultMap = new HashMap<>();
        if(StrUtil.isEmpty(text)){
            return resultMap;
        }
        try{
            for (String keyword : keywords) {
                //空关键词不做近似词匹配
                if(StrUtil.isEmpty(keyword)){
                    continue;
                }
                CommonSynonymDictionary.SynonymItem synonymItem = CoreSynonymDictionary.get(keyword);
                if(synonymItem != null && !CollectionUtils.isEmpty(synonymItem.synonymList)){
                    Map<String, Integer> synonymMap = new HashMap<>();
                    for (Synonym synonym : synonymItem.synonymList) {
                        String realWord = synonym.getRealWord();
                        //如果近似词为空,则不算结果集中
                        if(StrUtil.isEmpty(realWord)){
                            continue;
                        }
                        Integer count = keywordTimeCount(text, realWord);
                        if(count > 0){
                            synonymMap.put(realWord,count);
                        }
                    }
                    if(!CollectionUtils.isEmpty(synonymMap)){
                        resultMap.put(keyword,synonymMap);
                    }
                }
            }
        }catch (Exception e) {
            log.error("[synonymKeywords] 近似值匹配失败",e);
        }
        return resultMap;
    }

    /**
     * 余弦向量法 文本相似度计算
     * @param str1 文本1
     * @param str2 文本2
     * @return
     */
    public static double cosineSimilarity(String str1, String str2) {
        //算出文本向量
        Map<String, Integer> vector1 = getWordCount(str1);
        Map<String, Integer> vector2 = getWordCount(str2);
        if(vector1.size()==0 || vector2.size()==0){
            if(vector1.size()==vector2.size()){
                return 1;
            }else{
                return 0;
            }
        }
        // 计算余弦相似度
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (String word : vector1.keySet()) {
            if (vector2.containsKey(word)) {
                dotProduct += vector1.get(word) * vector2.get(word);
            }
            norm1 += Math.pow(vector1.get(word), 2);
        }
        for (String word : vector2.keySet()) {
            norm2 += Math.pow(vector2.get(word), 2);
        }
        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return similarity;
    }

    /**
     * 计算文本中分词和数量
     * @param text 计算文本
     * @return
     */
    public static Map<String, Integer> getWordCount(String text) {
        Map<String, Integer> wordCount = new HashMap<>();
        // 当 text 为空字符串时，使用分词函数会报错，所以需要提前处理这种情况
        if(StrUtil.isEmpty(text)){
            return wordCount;
        }
        List<Term> terms = HanLP.segment(text);
        for (Term term : terms) {
            String word = term.word;
            if (word.trim().length() > 0 && !isPunctuation(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        return wordCount;
    }


    /**
     * 判断关键词在文本中出现几次
     * @param text 文本
     * @param keyword 关键词
     * @return
     */
    private static Integer keywordTimeCount(String text,String keyword){
        int count = 0;
        String lowerCaseText = text.toLowerCase();
        String lowerCaseKeyword = keyword.toLowerCase();
        int index = lowerCaseText.indexOf(lowerCaseKeyword);
        while (index != -1) {
            count++;
            index = lowerCaseText.indexOf(lowerCaseKeyword, index + 1);
        }
        return count;
    }

    /**
     * 获取待匹配的文本内容
     * @param checkRange
     * @return
     */
    public static  String getMatchText(Byte checkRange,StringBuilder allText, StringBuilder serviceText, StringBuilder clientText){
        if(CheckRangeEnum.ALL.getCodeId() == checkRange){
            return allText.toString();
        }else if(CheckRangeEnum.WAITER.getCodeId() ==checkRange){
            return serviceText.toString();
        }else if(CheckRangeEnum.CUSTOMER.getCodeId() == checkRange) {
            return clientText.toString();
        }else {
            return "";
        }
    }

    /**
     * 将Map转换为List
     * @param map
     * @param ruleValue
     * @return
     */
    public static List<KeyWord.SpiltWord> convertMapToList(Map<String, Integer> map, Integer ruleValue, AtomicInteger count) {
        List<KeyWord.SpiltWord> resultList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer times = entry.getValue();
            KeyWord.SpiltWord spiltWord = new KeyWord.SpiltWord();
            spiltWord.setName(name);
            spiltWord.setTimes(times);
            spiltWord.setScore(ruleValue);
            count.addAndGet(times);
            resultList.add(spiltWord);
        }

        return resultList;
    }

    /**
     * 将Map转换
     * @param inputMap
     * @param ruleValue
     * @return
     */
    public static Map<String, List<KeyWord.SynonymWord>> convertMap(Map<String, Map<String, Integer>> inputMap,Integer ruleValue,AtomicInteger count) {
        Map<String, List<KeyWord.SynonymWord>> outputMap = new HashMap<>();

        for (String key : inputMap.keySet()) {
            Map<String, Integer> innerMap = inputMap.get(key);
            List<KeyWord.SynonymWord> synonymWords = new ArrayList<>();

            for (String innerKey : innerMap.keySet()) {
                String synonymName = innerKey;
                int synonymCount = innerMap.get(innerKey);
                count.addAndGet(synonymCount);
                KeyWord.SynonymWord synonymWord = new KeyWord.SynonymWord(synonymName, synonymCount,ruleValue);
                synonymWords.add(synonymWord);
            }
            outputMap.put(key, synonymWords);
        }
        return outputMap;
    }



    /**
     * 判断字符串是否是标点符号
     * @param str
     * @return
     */
    public static boolean isPunctuation(String str){
        return PUNCTUATION.contains(str);
    }





    public static void main(String[] args) {
//        String text = "这是一个测量文本，用于测试关键句匹配。";
//        String[] keywords = {"测试", "关键词"};
//        Map<String, Integer> result = countKeywords(text, keywords);
//        System.out.println("包含的测试关键词数量：" + result.get("测试"));
//        System.out.println("包含的关键词关键词数量：" + result.get("关键词"));


//        String a = "甲构成故意杀人(预备）法律法条和过失致人死亡罪。";
//        String b = "法律法规";
//        String c = getKeyWord(b,a,1);
//        System.out.println("取词结果为:" + c);


//        String e = "HanLP采用的数据预处理与拆分比例与流行方法未必相同，比如HanLP采用了完整版的MSRA命名实体识别语料，而非大众使用的阉割版；" +
//                "HanLP使用了语法覆盖更广的Stanford Dependencies标准，而非学术界沿用的Zhang and Clark (2008)标准；HanLP提出了均匀分割CTB的方法，" +
//                "而不采用学术界不均匀且遗漏了51个黄金文件的方法。HanLP开源了一整套语料预处理脚本与相应语料库，力图推动中文NLP的透明化。总之，HanLP只做我们认为正确、先进的事情，而不一定是流行、权威的事情";
//        String[] strArray = {"完好", "取名"};
//        Map<String, Map<String, Integer>> stringMapMap = synonymKeywords(e, strArray);
//        System.out.println(stringMapMap.toString());



        String str1 = "你吃饭了吗？如果没有吃饭的话，来我家吃吧";
        String str3 = "没有吃饭的话，我邀请你来我家吃饭";
        String str2 = "余弦相似度的计算单元是向量，因此第二个问题是如何将文本转换为向量";
        System.out.println(cosineSimilarity(str1, str3));

    }
}
