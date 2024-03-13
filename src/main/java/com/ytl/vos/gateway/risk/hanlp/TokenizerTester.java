package com.ytl.vos.gateway.risk.hanlp;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author alan.wang
 */
public class TokenizerTester {

    public static void main(String[] args){

        String text = "你好美丽的祖国大地，你好美丽的大好河山";
//        String text = "HanLP采用的数据预处理与拆分比例与流行方法未必相同，比如HanLP采用了完整版的MSRA命名实体识别语料，而非大众使用的阉割版；HanLP使用了语法覆盖更广的Stanford Dependencies标准，而非学术界沿用的Zhang and Clark (2008)标准；HanLP提出了均匀分割CTB的方法，而不采用学术界不均匀且遗漏了51个黄金文件的方法。HanLP开源了一整套语料预处理脚本与相应语料库，力图推动中文NLP的透明化。\n" +
//                "\n" +
//                "总之，HanLP只做我们认为正确、先进的事情，而不一定是流行、权威的事情。";
        List<Word> words = Tokenizer.segment(text);
        String wordStr = words.stream().map(word -> word.getName()).collect(Collectors.joining(" "));
        System.out.println(wordStr);
    }
}

