package com.example.auction.domain.ai.service;

import com.example.auction.domain.auction.entity.Auction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// 낙찰 경매의 상품명+설명 임베딩 저장 + 유사도 검색 — 상품 상태·스펙 기반 AI 분석에 활용
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionEmbeddingService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.4;

    private final VectorStore vectorStore;

    // 낙찰 경매 1건을 벡터로 변환해 pgvector에 저장 — 낙찰 스케줄러에서 호출
    // description이 없으면 상품명만으로는 의미 있는 벡터를 만들기 어려워 임베딩 대상에서 제외
    public void embed(Auction auction) {
        if (auction.getDescription() == null || auction.getDescription().isBlank()) {
            return;
        }

        String contextualText = "상품명: " + auction.getItemName() + ". 설명: " + auction.getDescription();

        Document document = new Document(
                contextualText,
                Map.of(
                        "source", "auction",
                        "auctionId", auction.getId(),
                        "itemName", auction.getItemName(),
                        "categoryId", auction.getCategoryId()
                )
        );
        vectorStore.add(List.of(document));
        log.debug("[AuctionEmbedding] 임베딩 저장 — auctionId={}", auction.getId());
    }

    // 상품 상태·스펙 관련 질문을 의미 유사도로 검색 — LLM 컨텍스트 주입용
    public List<String> search(String query) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .filterExpression("source == 'auction'")
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .toList();
    }
}