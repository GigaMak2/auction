package com.example.auction.domain.auction.search.util;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KoreanAnalyzerUtil {
    private final KoreanAnalyzer koreanAnalyzer;

    public String toTsVectorLiteral (String str) {
        if (str == null) {
            return "";
        }

        List<String> tokens = getTokens(str);

        // 토큰들을 tsvector literal로 변경
        
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<tokens.size(); i++) {
            String token = tokens.get(i);

            sb.append(prepareToken(token));
            sb.append(":");
            sb.append(i+1);
            sb.append(" ");
        }

        return sb.toString();
    }

    public String toTsQueryLiteral (String str) {
        if (str == null) {
            return "";
        }

        List<String> tokens = getTokens(str);

        // convert tokens to tsvector literal
        
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<tokens.size(); i++) {
            String token = tokens.get(i);

            sb.append(prepareToken(token));

            if (i + 1 < tokens.size()) {
                sb.append(" | ");
            }
        }

        return sb.toString();
    }

    private List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<>();

        try(TokenStream tokenStream = koreanAnalyzer.tokenStream("", str)) {

            CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);

            tokenStream.reset();

            while(tokenStream.incrementToken()) {
                tokens.add(charTermAttr.toString());
            }

            tokenStream.end();
        }catch(IOException e) {
            throw new RuntimeException(e);
        }

        return tokens;
    }

    private static String prepareToken(String str) {
        return "'" + str.replace("\\", "\\\\").replace("'", "''") +  "'";
    }
}
