package com.example.auction.domain.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contextual Retrieval 저장 텍스트 변환 확인 데모
 * Spring 컨텍스트, DB, 외부 API 불필요 — 언제든 실행 가능
 */
@DisplayName("Contextual Retrieval — 저장 텍스트 변환 데모")
class ContextualRetrievalDemoTest {

    @Test
    @DisplayName("별점 prepend 전후 pgvector 저장 텍스트 비교")
    void contextualRetrieval_저장텍스트_비교() {
        Object[][] samples = {
            {"배송 빠름",                   5},
            {"배송 빠름",                   1},   // 텍스트 동일, 별점만 다름
            {"배송 너무 느림. 일주일 걸림.", 1},
            {"상품 상태 사진과 달라 실망.",  2},
            {"포장 꼼꼼. 상품 완벽한 상태.", 5},
        };

        sep("Contextual Retrieval — 저장 텍스트 비교");
        System.out.printf("  %-5s | %-30s | %s%n", "별점", "원본 후기 텍스트", "pgvector 저장 텍스트");
        System.out.println("  " + "─".repeat(80));

        for (Object[] s : samples) {
            String raw   = (String) s[0];
            int    score = (int)    s[1];
            String stored = "별점: " + score + "점. 후기: " + raw;   // ReviewEmbeddingService.embed() 동일 로직
            System.out.printf("  %-5d | %-30s | %s%n", score, raw, stored);
        }

        System.out.println();
        System.out.println("  ▶ '배송 빠름' 텍스트가 동일해도 별점 정보가 붙으면");
        System.out.println("    → 5점짜리 vs 1점짜리가 다른 벡터로 저장됨");
        System.out.println("    → '좋은 후기 중 배송 빠른 것' 검색 시 5점짜리가 더 높은 유사도로 검색됨");
        sep(null);
    }

    private void sep(String title) {
        System.out.println();
        if (title != null) {
            System.out.println("  " + "═".repeat(80));
            System.out.println("    " + title);
            System.out.println("  " + "═".repeat(80));
        } else {
            System.out.println("  " + "═".repeat(80));
        }
    }
}
