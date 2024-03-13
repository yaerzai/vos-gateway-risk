package com.ytl.vos.gateway.risk.utils;



import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author kf-zhanghui
 * @date 2023/9/6 10:46
 */
public class Word2VecUtil {


    public static void main(String[] args) throws Exception {

        /**测试一
         * 结果：0.7624877691268921
         * */
//        String word1 = "信用卡和网贷有当前逾期的情况吗";
//        String word2 = "恭喜您活得了我们新资方5-10万的额度";
        /**测试二
         * 结果：
         0.9679021835327148
         * */
//        String word1 = "您房子是深圳本地的还是外地的？";
//        String word2 = "你是本地还是外地的？";

        /**测试三
         * 结果：0.9742866158485413
         * */
//        String word1 = "每个月对公流水的话是多少，有没有开票？";
//        String word2 = "每个月对公流水是多少，有没有开票？";

        /**
         * 测试四
         *0.8415305614471436
         */
//        String word1 = "全款还是按揭的呢";
//        String word2 = "是这样的我们行针对工薪阶层的贷款最高额度是200万，月息3-6厘，请问您有这方面需要吗";

        /**
         * 测试五
         *1.0000001192092896
         */
//        String word1 = "全款还是按揭的呢";
//        String word2 = "全款还是按揭的呢";


        /**
         * 测试六
         *0.6824735999107361
         */
        String word1 = "喂，您好";
        String word2 = "您好";


       //读取模型将模型加载为内存
        Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(new File("C:\\workProjects\\vos-plat\\vos-gateway-risk\\src\\main\\resources\\vectors-optimized.bin"));
        // 现在你可以使用 word2Vec 对象了，例如获取单词的向量表示：
//        double[] wordVector1 = word2Vec.getWordVector(word1);
//        System.out.println("wordVector1:" + wordVector1);
//        double[] wordVector2 = word2Vec.getWordVector(word2);
//        System.out.println("wordVector2:" + wordVector2);
//        INDArray sentence1Vector = word2Vec.getWordVectorMatrix(word1);
//        INDArray sentence2Vector = word2Vec.getWordVectorMatrix(word2);
        List<Term> segment1 = HanLP.segment(word1);
        List<String> sentence1Words = segment1.stream().map(item -> item.word).collect(Collectors.toList());
        //获取词列表的平均向量
        INDArray word1VectorsMean = word2Vec.getWordVectorsMean(sentence1Words);
        System.out.println("sentence1Words:" + sentence1Words);
        System.out.println("word1VectorsMean:" + word1VectorsMean);
        List<Term> segment2 = HanLP.segment(word2);
        List<String> sentence2Words = segment2.stream().map(item -> item.word).collect(Collectors.toList());
        //获取词列表的平均向量
        INDArray word2VectorsMean = word2Vec.getWordVectorsMean(sentence2Words);
        System.out.println("sentence2Words:" + sentence2Words);
        System.out.println("word2VectorsMean:" + word2VectorsMean);
//        ArrayList<INDArray> word1Vectors = new ArrayList<>();
//        for (int i = 0; i < sentence1Words.size(); i++) {
//            word1Vectors.add(word2Vec.getWordVectorMatrix(sentence1Words.get(i)));
//        }
//        ArrayList<INDArray> word2Vectors = new ArrayList<>();
//        for (int i = 0; i < sentence2Words.size(); i++) {
//            word2Vectors.add(word2Vec.getWordVectorMatrix(sentence2Words.get(i)));
//        }
//        Vectors.denseMean()

//        INDArray sentence1Vector = Nd4j.create(Math.max(sentence1Words.size(),sentence2Words.size()), 100, 'c');
//        INDArray sentence2Vector = Nd4j.create(Math.max(sentence1Words.size(),sentence2Words.size()), 100, 'c');
//        for (int i = 0; i < sentence1Words.size(); i++) {
//            String word = sentence1Words.get(i);
//            double[] vector = word2Vec.getWordVector(word);
//            for (int j = 0; j < 100; j++) {
//                sentence1Vector.putScalar(i, j, sentence1Vector.getDouble(i, j) + (vector != null ? vector[j] : 0));
//            }
//        }
//        for (int i = 0; i < sentence2Words.size(); i++) {
//            String word = sentence2Words.get(i);
//            double[] vector = word2Vec.getWordVector(word);
//            for (int j = 0; j < 100; j++) {
//                sentence2Vector.putScalar(i, j, sentence2Vector.getDouble(i, j) + (vector != null ? vector[j] : 0));
//            }
//        }
        //
        //计算两个句向量的余弦相似度
        double cosineSimilarity01 = Transforms.cosineSim(word1VectorsMean, word2VectorsMean);
        double cosineSimilarity02 = Nd4j.getBlasWrapper().level1().dot(100,1.0,word1VectorsMean, word2VectorsMean)
                / (word1VectorsMean.norm2(0).mul(word2VectorsMean.norm2(0)).getDouble(0));

        System.out.println("cosineSimilarity01:"+cosineSimilarity01); // 打印余弦相似度结果
        System.out.println("cosineSimilarity02:"+cosineSimilarity02);

    }
}
