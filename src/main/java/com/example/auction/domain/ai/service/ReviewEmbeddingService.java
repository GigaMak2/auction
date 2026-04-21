package com.example.auction.domain.ai.service;

import com.example.auction.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// 후기 텍스트 임베딩 저장 + 유사도 검색 — 판매자 신뢰도 RAG 분석에 활용
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEmbeddingService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.4;
    private static final int HYDE_TIMEOUT_SECONDS = 5;
    private static final int HYDE_CACHE_MAX_SIZE = 50;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    // 동일 쿼리 반복 호출 시 LLM 재호출 방지 — LRU 방식으로 최대 50개 유지
    private final Map<String, String> hydeCache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > HYDE_CACHE_MAX_SIZE;
                }
            }
    );

    // 후기 1건을 벡터로 변환해 pgvector에 저장 — 리뷰 생성 시 호출
    // Contextual Retrieval: 별점을 텍스트에 prepend해 짧은 후기의 임베딩 품질 개선
    public void embed(Review review) {
        if (review.getDescription() == null || review.getDescription().isBlank()) {
            return;
        }

        String contextualText = "별점: " + review.getScore() + "점. 후기: " + review.getDescription();

        Document document = new Document(
                contextualText,
                Map.of(
                        "sellerId", review.getRevieweeId(),
                        "score", review.getScore(),
                        "reviewId", review.getId()
                )
        );
        vectorStore.add(List.of(document));
    }

    // sellerId 필터 + 의미 유사도 기반 후기 텍스트 검색 — LLM 컨텍스트 주입용
    // HyDE: 유저 질문으로 가상 후기를 생성한 뒤 그 임베딩으로 검색 — 질문↔후기 문체 차이 완화
    public List<String> search(Long sellerId, String query) {
        String searchQuery = generateHypotheticalReview(query);

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .filterExpression("sellerId == " + sellerId)
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .toList();
    }

    private String generateHypotheticalReview(String query) {
        String cached = hydeCache.get(query);
        if (cached != null) {
            log.debug("[RAG] HyDE 캐시 히트 — queryLength={}", query.length());
            return cached;
        }

        try {
            String result = CompletableFuture
                    .supplyAsync(() -> chatModel.call("""
                            당신은 중고 경매 플랫폼의 판매자 후기를 생성하는 시스템입니다.
                            사용자 질문을 보고, 실제 있을 법한 한국어 후기 문장을 3개 생성하세요.
                            짧은 후기 스타일로 작성하세요 (예: "배송 빠름", "포장 꼼꼼함", "상품 상태 양호").
                            질문에서 암시된 항목(배송 속도, 포장 상태, 상품 상태 등)을 반영하세요.
                            판매자 ID나 주문 번호는 포함하지 마세요.

                            질문: "%s"

                            후기:
                            """.formatted(query)))
                    .get(HYDE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            hydeCache.put(query, result);
            log.debug("[RAG] HyDE 가상 후기 생성 성공 — queryLength={}", query.length());
            return result;
        } catch (Exception e) {
            log.warn("[RAG] HyDE 가상 후기 생성 실패, 원본 쿼리로 폴백: {}", e.getMessage());
            return query;
        }
    }
}
