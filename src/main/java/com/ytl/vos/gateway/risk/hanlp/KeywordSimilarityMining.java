package com.ytl.vos.gateway.risk.hanlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;
import com.hankcs.hanlp.dictionary.common.CommonSynonymDictionary;
import com.hankcs.hanlp.seg.common.Term;

import java.util.List;

public class KeywordSimilarityMining {
    public static void main(String[] args) {
        // 要处理的文本内容
        String text = "这是要处理的文本内容";
        
        // 对文本进行分词
        List<Term> termList = HanLP.segment(text);
        
        // 输出分词结果
        for (Term term : termList) {
            System.out.println(term.word);
        }
        
        // 获取关键词
        List<String> keywords = HanLP.extractKeyword(text, 10);
        
        // 输出关键词
        for (String keyword : keywords) {
            System.out.println(keyword);
        }
        
        // 获取关键词的相似词语
        for (String keyword : keywords) {
            CommonSynonymDictionary.SynonymItem synonymItem = CoreSynonymDictionary.get(keyword);
//            List<String> similarWords = HanLP.CoreSynonymDictionary(keyword, 10);
            System.out.println(synonymItem.toString());
            // 输出相似词语
//            for (String similarWord : similarWords) {
//                System.out.println(similarWord);
//            }
        }
    }
}
