package com.ytl.vos.gateway.risk.utils;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by qianyang on 8/28/2018.
 */
public class Test {

    private static Logger log = LoggerFactory.getLogger(Test.class);
    private static String outputPath = "E:/word2vec.txt";
    public static void main(String[] args) throws Exception {
        //输入文本文件的目录
        File inputTxt = new File("E:/raw_sentences.txt");
        log.info("开始加载数据...."+inputTxt.getName());
        //加载数据
        SentenceIterator iter = new LineSentenceIterator(inputTxt);
        //切词操作
        TokenizerFactory token = new DefaultTokenizerFactory();
        //去除特殊符号及大小写转换操作
        token.setTokenPreProcessor(new CommonPreprocessor());
        log.info("训练模型....");
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)//词在语料中必须出现的最少次数
                .iterations(1)
                .layerSize(100)  //向量维度
                .seed(42)
                .windowSize(10) //窗口大小
                .iterate(iter)
                .tokenizerFactory(token)
                .build();
        log.info("配置模型....");
        vec.fit();
        log.info("输出词向量....");
        WordVectorSerializer.writeWordVectors(vec, outputPath);
        log.info("相似的词:");
        //获取相似的词
        Collection<String> lst = vec.wordsNearest("day", 10);
        System.out.println(lst);
        //获取某词对应的向量
        log.info("向量获取:");
        double[] wordVector = vec.getWordVector("day");
        System.out.println(Arrays.toString(wordVector));
    }
}
