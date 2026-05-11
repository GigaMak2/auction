package com.example.auction.common.config;

import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KoreanAnalyzerConfig {

    @Bean
    public KoreanAnalyzer koreanAnalyzer() {
        return new KoreanAnalyzer();
    }
}
