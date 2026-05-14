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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionEmbeddingService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.4;

    private final VectorStore vectorStore;

    // description이 없으면 의미 있는 벡터 생성 불가 - 임베딩 대상 제외
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
    }

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