package com.example.auction.domain.ai.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.auction.domain.auction.eventBridge.AuctionEventBridgeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HyDE + Contextual Retrieval 효과를 실제 pgvector 검색 결과로 확인하는 데모
 * 실행 전 docker-compose-test.yml (PostgreSQL+pgvector) 기동 필요
 * 실행: ./gradlew test --tests "*.RagComparisonDemoTest" --info
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("RAG 기법 효과 비교 — HyDE + Contextual Retrieval")
class RagComparisonDemoTest {

    @Autowired private VectorStore vectorStore;
    @Autowired private ChatModel   chatModel;

    @MockitoBean private AuctionEventBridgeService auctionEventBridgeService;

    private static final long TEST_SELLER_ID = 99999L;

    private static final String HYDE_PROMPT = """
            당신은 중고 경매 플랫폼의 판매자 후기를 생성하는 시스템입니다.
            사용자 질문을 보고, 실제 있을 법한 한국어 후기 문장을 3개 생성하세요.
            짧은 후기 스타일로 작성하세요 (예: "배송 빠름", "포장 꼼꼼함", "상품 상태 양호").
            질문에서 암시된 항목(배송 속도, 포장 상태, 상품 상태 등)을 반영하세요.
            판매자 ID나 주문 번호는 포함하지 마세요.

            질문: "%s"

            후기:
            """;

    private final List<String> insertedIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        if (!insertedIds.isEmpty()) {
            vectorStore.delete(insertedIds);
            insertedIds.clear();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. HyDE — 유저 질문이 어떻게 가상 후기로 변환되는지 (DeepSeek API 필요)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("1. HyDE — 질문 → 가상 후기 변환 결과 확인")
    void hyde_쿼리변환_비교() {
        String[] queries = {
            "이 판매자 배송이 빠른가요?",
            "상품이 사진과 많이 다른가요?",
            "이 판매자 믿을 수 있나요?",
        };

        sep("HyDE — 질문 → 가상 후기 변환 결과");

        for (String query : queries) {
            String hyde = chatModel.call(HYDE_PROMPT.formatted(query));

            System.out.println("  [ 유저 질문 원문 ]");
            System.out.println("    \"" + query + "\"");
            System.out.println("  → 이 문장 그대로 임베딩하면 후기 문체와 달라 유사도 낮음");
            System.out.println();
            System.out.println("  [ HyDE 변환 결과 — 실제로 pgvector 검색에 사용되는 텍스트 ]");
            String safeHyde = hyde != null ? hyde.strip() : "";
            for (String line : safeHyde.split("\n")) {
                if (!line.isBlank()) System.out.println("    " + line);
            }
            System.out.println("  " + "─".repeat(80));
        }

        sep(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. 검색 결과 — 동일 후기 세트에서 HyDE 적용 전후 유사도 점수 비교
    //    (pgvector + DeepSeek API 필요)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("2. 검색결과 비교 — HyDE 적용 전후 유사도 점수 차이")
    void 검색결과_HyDE적용전후_비교() {
        List<String[]> reviews = List.of(
            new String[]{"배송 빠름. 포장 깔끔.",              "5"},
            new String[]{"배송 엄청 느림. 일주일 이상 걸림.",  "1"},
            new String[]{"설명과 다른 상품. 흠집 심함.",        "2"},
            new String[]{"포장 꼼꼼. 상품 완벽 그 자체.",      "5"},
            new String[]{"연락 안 받음. 환불 거부.",           "1"},
            new String[]{"빠른 배송 감사합니다. 재구매 예정.", "4"},
            new String[]{"상품 상태 보통. 설명대로.",          "3"},
            new String[]{"새 제품처럼 깔끔히 포장되어 있었음.", "5"}
        );

        for (String[] r : reviews) {
            String id   = UUID.randomUUID().toString();
            String text = "별점: " + r[1] + "점. 후기: " + r[0];  // Contextual Retrieval 적용
            vectorStore.add(List.of(new Document(id, text,
                    Map.of("sellerId", TEST_SELLER_ID, "score", Integer.parseInt(r[1])))));
            insertedIds.add(id);
        }

        String userQuery = "이 판매자 배송 빠른가요?";
        String filter    = "sellerId == " + TEST_SELLER_ID;

        List<Document> withoutHyde = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuery)
                        .topK(5)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build()
        );

        String hydeQuery = chatModel.call(HYDE_PROMPT.formatted(userQuery));
        String safeHydeQuery = hydeQuery != null ? hydeQuery : userQuery;

        List<Document> withHyde = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(safeHydeQuery)
                        .topK(5)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build()
        );

        sep("검색결과 비교 — 질문: \"" + userQuery + "\"");

        System.out.println("  ── HyDE 미적용 (원본 질문 그대로 검색) ──────────────────────────");
        System.out.println("  검색 입력: \"" + userQuery + "\"");
        System.out.println();
        withoutHyde.forEach(d ->
                System.out.printf("  [유사도 %.4f]  %s%n", score(d), d.getText()));

        System.out.println();
        System.out.println("  ── HyDE 적용 (가상 후기로 변환 후 검색) ─────────────────────────");
        System.out.println("  검색 입력 (변환 후):");
        for (String line : safeHydeQuery.strip().split("\n")) {
            if (!line.isBlank()) System.out.println("    " + line);
        }
        System.out.println();
        withHyde.forEach(d ->
                System.out.printf("  [유사도 %.4f]  %s%n", score(d), d.getText()));

        System.out.println();
        System.out.println("  ▶ 배송 관련 후기('배송 빠름', '빠른 배송 감사합니다')의 유사도가");
        System.out.println("    HyDE 적용 시 더 높게 나타나는지 확인 포인트");
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

    private double score(Document d) {
        return d.getScore() != null ? d.getScore() : 0.0;
    }
}
