package com.example.auction.domain.ai.service;

import com.example.auction.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 후기 텍스트 임베딩 저장 + 유사도 검색 — 판매자 신뢰도 RAG 분석에 활용
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEmbeddingService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.4;
    private static final int HYDE_TIMEOUT_SECONDS = 5;
    private static final int HYDE_CACHE_MAX_SIZE = 50;

    // 부정 쿼리 감지 키워드 — 매칭 시 score <= 2 필터 적용으로 부정 후기만 검색
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "오래 걸", "너무 늦", "불량", "최악", "연락두절", "사기", "불만", "실망", "안 되", "못 받", "느리"
    );

    // 긍정 쿼리 감지 키워드 — 매칭 시 score >= 4 필터 적용으로 긍정 후기만 검색
    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "좋은", "빠른", "빠르", "친절", "정확", "깔끔", "만족", "훌륭", "완벽", "추천", "믿을"
    );

    // blocking HTTP 호출 전용 가상 스레드 Executor — Tomcat 요청 스레드와 격리
    private static final ExecutorService hydeExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // 동일 쿼리 반복 호출 시 LLM 재호출 방지 — LRU 방식으로 최대 50개 유지
    private final Map<String, String> hydeCache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > HYDE_CACHE_MAX_SIZE;
                }
            }
    );

    // 후기 1건을 벡터로 변환해 pgvector에 저장 — 리뷰 생성/수정 시 호출
    // Contextual Retrieval: 별점을 텍스트에 prepend해 짧은 후기의 임베딩 품질 개선
    // reviewId를 Document ID로 고정해 동일 ID upsert로 수정 시 덮어쓰기 보장
    public void embed(Review review) {
        if (review.getDescription() == null || review.getDescription().isBlank()) {
            return;
        }

        String contextualText = "별점: " + review.getScore() + "점. 후기: " + review.getDescription();

        Document document = new Document(
                toDocId(review.getId()),
                contextualText,
                Map.of(
                        "source", "review",
                        "sellerId", review.getRevieweeId(),
                        "score", review.getScore(),
                        "reviewId", review.getId()
                )
        );
        vectorStore.add(List.of(document));
    }

    // 후기 삭제 시 pgvector에서 해당 벡터 제거
    public void delete(Long reviewId) {
        vectorStore.delete(List.of(toDocId(reviewId)));
    }

    private static String toDocId(Long reviewId) {
        return UUID.nameUUIDFromBytes(("review:" + reviewId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    // sellerId 필터 + 의미 유사도 기반 후기 텍스트 검색 — LLM 컨텍스트 주입용
    // HyDE: 유저 질문으로 가상 후기를 생성한 뒤 그 임베딩으로 검색 — 질문↔후기 문체 차이 완화
    // 쿼리 방향성(긍정/부정) 감지 → score 필터로 감성 불일치 후기 사전 차단
    public List<String> search(Long sellerId, String query) {
        String searchQuery = generateHypotheticalReview(query);
        boolean isNegative = NEGATIVE_KEYWORDS.stream().anyMatch(query::contains);
        boolean isPositive = !isNegative && POSITIVE_KEYWORDS.stream().anyMatch(query::contains);
        String scoreFilter = isNegative ? " AND score <= 2" : isPositive ? " AND score >= 4" : "";

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .filterExpression("sellerId == " + sellerId + scoreFilter)
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .toList();
    }

    // JSON 배열 형식으로 출력 강제 — 설명 줄/번호가 임베딩을 오염시키는 문제 차단
    private String generateHypotheticalReview(String query) {
        String cached = hydeCache.get(query);
        if (cached != null) {
            log.debug("[RAG] HyDE 캐시 히트 — queryLength={}", query.length());
            return cached;
        }

        try {
            String raw = CompletableFuture
                    .supplyAsync(() -> chatModel.call("""
                            당신은 중고 경매 플랫폼의 판매자 후기를 생성하는 시스템입니다.
                            사용자 질문이 긍정적 경험을 찾는지, 부정적 경험을 찾는지 방향성을 파악하세요.
                            파악한 방향에 맞는 실제 있을 법한 한국어 후기 문장 3개를 JSON 배열로만 출력하세요.
                            반드시 다음 형식을 지키세요: ["후기1", "후기2", "후기3"]
                            JSON 배열 이외에 어떠한 텍스트도 출력하지 마세요.
                            짧은 후기 스타일로 작성하세요.
                            긍정 예시: ["배송 빠름", "포장 꼼꼼함", "상품 상태 양호"]
                            부정 예시: ["배송 느림", "연락두절", "상품 불량"]
                            질문에서 암시된 항목(배송 속도, 포장 상태, 상품 상태 등)을 반영하세요.
                            판매자 ID나 주문 번호는 포함하지 마세요.

                            질문: "%s"
                            """.formatted(query)), hydeExecutor)
                    .get(HYDE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String result = parseAndJoin(raw, query);
            hydeCache.put(query, result);
            log.debug("[RAG] HyDE 가상 후기 생성 성공 — queryLength={}", query.length());
            return result;
        } catch (Exception e) {
            log.warn("[RAG] HyDE 가상 후기 생성 실패, 원본 쿼리로 폴백: {}", e.getMessage());
            return query;
        }
    }

    // JSON 추출 → cleanHydeResult → 원본 쿼리 순으로 폴백
    // LLM이 JSON 외 텍스트를 앞뒤에 붙이는 경우에도 배열 부분만 추출해 파싱
    private String parseAndJoin(String raw, String fallback) {
        try {
            String json = raw.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            List<String> reviews = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (reviews != null && !reviews.isEmpty()) {
                return String.join(" ", reviews);
            }
        } catch (Exception e) {
            log.warn("[RAG] HyDE JSON 파싱 실패, 후처리로 폴백: {}", e.getMessage());
        }

        String cleaned = cleanHydeResult(raw);
        if (cleaned != null) {
            log.debug("[RAG] HyDE 후처리 성공 — length={}", cleaned.length());
            return cleaned;
        }

        log.warn("[RAG] HyDE 후처리도 실패, 원본 쿼리로 폴백");
        return fallback;
    }

    // LLM이 번호(1. 2.)나 설명 줄을 붙인 경우 제거 — 실제 후기 문장만 추출해 join
    private String cleanHydeResult(String raw) {
        String result = Arrays.stream(raw.split("\n"))
                .map(String::strip)
                .filter(l -> !l.isBlank())
                .filter(l -> !l.endsWith("입니다.") && !l.endsWith("입니다"))
                .filter(l -> !l.endsWith(":"))
                .filter(l -> !l.contains("질문") && !l.contains("생성된"))
                .map(l -> l.replaceAll("^\\d+[.)\\s]+", "").strip())
                .map(l -> l.replaceAll("^\"+|\"+$", "").strip())
                .filter(l -> l.length() >= 3)
                .collect(Collectors.joining(" "));
        return result.isBlank() ? null : result;
    }
}
