package com.example.auction.domain.ai.service;

import com.example.auction.domain.auction.eventBridge.AuctionEventBridgeService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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

    @Autowired private VectorStore             vectorStore;
    @Autowired private ChatModel               chatModel;
    @Autowired private ReviewEmbeddingService  reviewEmbeddingService;

    @MockitoBean private AuctionEventBridgeService auctionEventBridgeService;

    private static final long SELLER_A = 99991L;  // 별점 포함 (Contextual Retrieval 적용)
    private static final long SELLER_B = 99992L;  // 별점 미포함 (비교 대조군)

    // [개선] 방향성(긍정/부정) 파악 지시 추가 — "배송이 오래 걸리나요?" 같은 부정 쿼리에도 올바른 방향 후기 생성
    private static final String HYDE_PROMPT = """
            당신은 중고 경매 플랫폼의 판매자 후기를 생성하는 시스템입니다.
            먼저 사용자 질문이 긍정적 경험을 찾는지, 부정적 경험을 찾는지 방향성을 파악하세요.
            파악한 방향에 맞는 실제 있을 법한 한국어 후기 문장을 3개 생성하세요.
            짧은 후기 스타일로 작성하세요.
            긍정 예시: "배송 빠름", "포장 꼼꼼함", "상품 상태 양호"
            부정 예시: "배송 느림", "연락두절", "상품 불량"
            질문에서 암시된 항목(배송 속도, 포장 상태, 상품 상태 등)을 반영하세요.
            판매자 ID나 주문 번호는 포함하지 마세요.

            질문: "%s"

            후기:
            """;

    private final List<String> insertedIds = new ArrayList<>();

    record LabeledReview(String text, int score, String topic) {}
    record QueryScenario(String query, String relevantTopic, String label) {}

    @AfterEach
    void cleanup() {
        if (!insertedIds.isEmpty()) {
            vectorStore.delete(insertedIds);
            insertedIds.clear();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. HyDE — 질문체 → 후기체 변환 결과 출력 (방향성 반영 확인)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("1. HyDE — 질문 → 가상 후기 변환 결과 확인 (방향성 반영)")
    void hyde_쿼리변환_비교() {
        String[] queries = {
            "이 판매자 배송이 빠른가요?",        // 긍정 탐색
            "배송이 오래 걸리거나 지연된 경우가 있나요?",  // 부정 탐색 — 이전엔 실패
            "이 판매자 믿을 수 있나요?",
        };

        sep("HyDE — 질문체 → 후기체 변환 결과 (방향성 반영 개선 후)");
        System.out.println("  ▶ 유저 질문(의문문)은 후기(평가문)와 임베딩 공간에서 거리가 발생");
        System.out.println("  ▶ 개선: 긍정/부정 질문 방향성을 파악해 그에 맞는 후기 생성");
        System.out.println();

        for (String query : queries) {
            String hyde     = chatModel.call(HYDE_PROMPT.formatted(query));
            String safeHyde = hyde != null ? hyde.strip() : "";

            System.out.println("  ┌── 유저 질문 ─────────────────────────────────────────────────");
            System.out.println("  │  \"" + query + "\"");
            System.out.println("  ├── HyDE 변환 결과 ────────────────────────────────────────────");
            for (String line : safeHyde.split("\n")) {
                if (!line.isBlank()) System.out.println("  │  " + line);
            }
            System.out.println("  └──────────────────────────────────────────────────────────────");
            System.out.println();
        }

        sep(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Precision@3 비교 — 방향성 개선 후 HyDE 평균 정밀도
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("2. Precision@3 비교 — HyDE 방향성 개선 후 평균 정밀도")
    void precision_HyDE_비교() {
        List<LabeledReview> dataset = List.of(
            new LabeledReview("배송 빠름. 하루 만에 도착.",            5, "delivery_pos"),
            new LabeledReview("당일 발송. 다음날 바로 수령.",          5, "delivery_pos"),
            new LabeledReview("빠른 배송 감사합니다. 재구매 예정.",    4, "delivery_pos"),
            new LabeledReview("포장 꼼꼼, 배송도 빨라서 만족.",       5, "delivery_pos"),
            new LabeledReview("이틀 만에 도착. 신속 배송.",           4, "delivery_pos"),
            new LabeledReview("주문 다음날 수령. 역대급 빠른 배송.",   5, "delivery_pos"),

            new LabeledReview("배송 엄청 느림. 일주일 이상 걸림.",     1, "delivery_neg"),
            new LabeledReview("발송 연락도 없음. 배송 10일 걸림.",     1, "delivery_neg"),
            new LabeledReview("주문 후 5일 지나서야 발송. 실망.",      2, "delivery_neg"),
            new LabeledReview("배송 지연됨. 추적도 안 됨.",            1, "delivery_neg"),

            new LabeledReview("상품 설명대로. 상태 양호.",             4, "item"),
            new LabeledReview("사진과 달리 흠집 심함. 환불 요청.",     1, "item"),
            new LabeledReview("실제 상품이 사진보다 훨씬 좋음.",       5, "item"),
            new LabeledReview("설명과 다른 상품 상태. 실망.",          2, "item"),
            new LabeledReview("새 제품처럼 깔끔히 포장되어 있었음.",   5, "item"),

            new LabeledReview("소통 원활. 친절한 판매자.",             5, "trust"),
            new LabeledReview("연락 안 받음. 환불 거부.",              1, "trust"),
            new LabeledReview("빠른 답변. 신뢰할 수 있는 판매자.",     5, "trust"),
            new LabeledReview("사기꾼 주의. 연락두절.",                1, "trust"),
            new LabeledReview("친절하고 정직한 판매자. 재구매 의향.",   5, "trust")
        );

        for (LabeledReview r : dataset) {
            String id   = UUID.randomUUID().toString();
            String text = "별점: " + r.score() + "점. 후기: " + r.text();
            vectorStore.add(List.of(new Document(id, text,
                    Map.of("sellerId", SELLER_A, "topic", r.topic(), "score", r.score()))));
            insertedIds.add(id);
        }

        String filter = "sellerId == " + SELLER_A;

        List<QueryScenario> scenarios = List.of(
            new QueryScenario("배송이 빠른가요?",                      "delivery_pos", "긍정 배송"),
            new QueryScenario("배송이 오래 걸리거나 지연되나요?",       "delivery_neg", "부정 배송"),
            new QueryScenario("상품 상태가 설명과 같나요?",            "item",         "상품 상태"),
            new QueryScenario("이 판매자 소통이 잘 되나요?",           "trust",        "판매자 신뢰")
        );

        sep("Precision@3 비교 — HyDE 방향성 개선 전/후");
        System.out.printf("  %-38s  %-14s  %-14s  %s%n",
                "쿼리", "P@3(HyDE 미적용)", "P@3(HyDE 적용)", "변화");
        System.out.println("  " + "─".repeat(82));

        double totalWithout = 0, totalWith = 0;

        for (QueryScenario s : scenarios) {
            List<Document> without = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(s.query()).topK(3)
                            .similarityThreshold(0.0).filterExpression(filter).build()
            );

            String hydeQuery = chatModel.call(HYDE_PROMPT.formatted(s.query()));
            String safeHyde  = hydeQuery != null ? hydeQuery : s.query();
            List<Document> with = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(safeHyde).topK(3)
                            .similarityThreshold(0.0).filterExpression(filter).build()
            );

            double pWithout = precision(without, s.relevantTopic(), dataset);
            double pWith    = precision(with,    s.relevantTopic(), dataset);
            totalWithout += pWithout;
            totalWith    += pWith;

            String change = pWith > pWithout ? String.format("▲ +%.2f", pWith - pWithout)
                          : pWith < pWithout ? String.format("▼ -%.2f", pWithout - pWith)
                          : "= 동일";
            System.out.printf("  %-38s  %-14.2f  %-14.2f  %s%n",
                    s.query(), pWithout, pWith, change);
        }

        double avgWithout = totalWithout / scenarios.size();
        double avgWith    = totalWith    / scenarios.size();
        System.out.println("  " + "─".repeat(82));
        System.out.printf("  %-38s  %-14.2f  %-14.2f  %s%n",
                "평균 Precision@3", avgWithout, avgWith,
                avgWith >= avgWithout
                        ? String.format("▲ +%.2f", avgWith - avgWithout)
                        : String.format("▼ -%.2f", avgWithout - avgWith));
        System.out.println();
        System.out.println("  ▶ 이전 결과: 평균 0.83 → 0.58 (HyDE가 부정 쿼리를 잘못 처리)");
        System.out.println("  ▶ 개선 후: 부정 쿼리에서도 방향성에 맞는 후기 생성 → 평균 상승 기대");
        sep(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Contextual Retrieval — 짧고 모호한 후기에 별점 맥락 추가 효과
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("3. Contextual Retrieval — 짧고 모호한 후기에 별점 맥락 추가 효과")
    void contextual_retrieval_극적효과_비교() {
        // 핵심 설계: 텍스트가 짧고 중립적 → 별점 없이는 긍/부정 판단 불가
        // "받았습니다" ★5 vs "받았습니다" ★1 — 텍스트만으로는 동일, 별점이 유일한 감성 신호
        // SELLER_A: "별점: N점. 후기: 텍스트" (Contextual Retrieval 적용)
        // SELLER_B: 텍스트만 (별점 정보 없음)
        List<String[]> reviews = List.of(
            new String[]{"받았습니다.",       "5"},
            new String[]{"받았습니다.",       "1"},
            new String[]{"배송 완료.",        "5"},
            new String[]{"배송 완료.",        "1"},
            new String[]{"거래 완료.",        "4"},
            new String[]{"거래 완료.",        "2"},
            new String[]{"도착.",             "5"},
            new String[]{"도착.",             "1"},
            new String[]{"물건 받음.",        "4"},
            new String[]{"물건 받음.",        "2"},
            new String[]{"수령.",             "5"},
            new String[]{"수령.",             "1"},
            new String[]{"확인.",             "4"},
            new String[]{"확인.",             "2"},
            new String[]{"처리됨.",           "5"},
            new String[]{"처리됨.",           "1"}
        );

        for (String[] r : reviews) {
            String idA = UUID.randomUUID().toString();
            vectorStore.add(List.of(new Document(idA,
                    "별점: " + r[1] + "점. 후기: " + r[0],
                    Map.of("sellerId", SELLER_A, "score", Integer.parseInt(r[1])))));
            insertedIds.add(idA);

            String idB = UUID.randomUUID().toString();
            vectorStore.add(List.of(new Document(idB, r[0],
                    Map.of("sellerId", SELLER_B, "score", Integer.parseInt(r[1])))));
            insertedIds.add(idB);
        }

        // 쿼리 A: 감성 쿼리 — 별점 텍스트 없음 → 효과 약함 (비교 대조군)
        String emotionPosQuery = "정말 만족스러운 좋은 거래 경험이었나요?";
        String emotionNegQuery = "불만족스럽고 나쁜 거래 경험이 있나요?";

        // 쿼리 B: 별점 직접 언급 — "별점: N점" 텍스트가 임베딩에서 직접 매칭 → 효과 극대화
        String scorePosQuery = "별점 5점 혹은 4점짜리 높은 평점 후기가 있나요?";
        String scoreNegQuery = "별점 1점 혹은 2점짜리 낮은 평점 불만 후기가 있나요?";

        int topK = 8;

        List<Document> emotionPosA = search(emotionPosQuery, "sellerId == " + SELLER_A, topK);
        List<Document> emotionPosB = search(emotionPosQuery, "sellerId == " + SELLER_B, topK);
        List<Document> emotionNegA = search(emotionNegQuery, "sellerId == " + SELLER_A, topK);
        List<Document> emotionNegB = search(emotionNegQuery, "sellerId == " + SELLER_B, topK);

        List<Document> scorePosA = search(scorePosQuery, "sellerId == " + SELLER_A, topK);
        List<Document> scorePosB = search(scorePosQuery, "sellerId == " + SELLER_B, topK);
        List<Document> scoreNegA = search(scoreNegQuery, "sellerId == " + SELLER_A, topK);
        List<Document> scoreNegB = search(scoreNegQuery, "sellerId == " + SELLER_B, topK);

        sep("Contextual Retrieval — 짧고 모호한 후기에 별점 맥락 추가 효과");
        System.out.println("  ▶ 16개 후기 — 텍스트는 짧고 중립적(\"받았습니다\", \"배송 완료\" 등), 별점만 다름");
        System.out.println("  ▶ 별점 미포함: 텍스트가 동일하므로 긍/부정 구분 불가 → 상위 결과가 뒤섞임");
        System.out.println("  ▶ 별점 포함: \"별점: N점\" 텍스트가 임베딩에 실려 방향성 분리");
        System.out.println();

        System.out.println("  ── [감성 쿼리] 별점 텍스트 없이 의미만으로 탐색 ──────────────────────");
        System.out.println("  ▶ 예상: 텍스트가 중립적이라 별점 포함 여부 효과 미미");
        printComparison(emotionPosQuery, emotionPosB, emotionPosA, topK, true);
        printComparison(emotionNegQuery, emotionNegB, emotionNegA, topK, false);

        System.out.println();
        System.out.println("  ── [별점 직접 언급 쿼리] \"별점 N점\"을 쿼리에 포함 ─────────────────────");
        System.out.println("  ▶ 예상: 별점 포함 데이터의 \"별점: N점\" 텍스트와 직접 매칭 → 극적 차이");
        printComparison(scorePosQuery, scorePosB, scorePosA, topK, true);
        printComparison(scoreNegQuery, scoreNegB, scoreNegA, topK, false);

        System.out.println();
        System.out.println("  ▶ Contextual Retrieval 핵심: 짧고 맥락 없는 후기도 별점이 붙으면");
        System.out.println("    별점 언급 쿼리에서 정확히 필터링 가능 — 감성 쿼리보다 별점 쿼리에서 효과 극대화");
        sep(null);
    }

    private void printComparison(String query, List<Document> without, List<Document> with,
                                  int topK, boolean positiveExpected) {
        double avgWithout = without.stream().mapToInt(this::score).average().orElse(0);
        double avgWith    = with.stream().mapToInt(this::score).average().orElse(0);
        long highWithout  = without.stream().filter(d -> score(d) >= 4).count();
        long highWith     = with.stream().filter(d -> score(d) >= 4).count();
        long lowWithout   = without.stream().filter(d -> score(d) <= 2).count();
        long lowWith      = with.stream().filter(d -> score(d) <= 2).count();

        System.out.println();
        System.out.printf("  쿼리: \"%s\"%n", query);
        System.out.printf("  %-30s  %-12s  %-12s  %s%n",
                "", "별점 미포함", "별점 포함", "변화");
        System.out.println("  " + "─".repeat(65));
        System.out.printf("  %-30s  %-12.2f  %-12.2f  %s%n",
                "상위 " + topK + "개 평균 별점",
                avgWithout, avgWith,
                avgWith > avgWithout ? String.format("▲ +%.2f", avgWith - avgWithout)
                        : avgWith < avgWithout ? String.format("▼ -%.2f", avgWithout - avgWith)
                        : "= 동일");
        if (positiveExpected) {
            System.out.printf("  %-30s  %-12d  %-12d  %s%n",
                    "★4+ (고점수) 개수",
                    highWithout, highWith,
                    highWith > highWithout ? "▲ +" + (highWith - highWithout) + " ✓"
                            : highWith < highWithout ? "▼ -" + (highWithout - highWith)
                            : "= 동일");
        } else {
            System.out.printf("  %-30s  %-12d  %-12d  %s%n",
                    "★1-2 (저점수) 개수",
                    lowWithout, lowWith,
                    lowWith > lowWithout ? "▲ +" + (lowWith - lowWithout) + " ✓"
                            : lowWith < lowWithout ? "▼ -" + (lowWithout - lowWith)
                            : "= 동일");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. 유사도 상세 — 전체 순위 + 배송 긍정 후기 평균 순위 비교
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("4. 유사도 상세 — 배송 쿼리 전체 순위 변화")
    void 유사도_상세_배송쿼리() {
        List<String[]> reviews = List.of(
            new String[]{"배송 빠름. 포장 깔끔.",              "5"},
            new String[]{"배송 엄청 느림. 일주일 이상 걸림.",  "1"},
            new String[]{"설명과 다른 상품. 흠집 심함.",        "2"},
            new String[]{"포장 꼼꼼. 상품 완벽 그 자체.",      "5"},
            new String[]{"연락 안 받음. 환불 거부.",           "1"},
            new String[]{"빠른 배송 감사합니다. 재구매 예정.", "4"},
            new String[]{"상품 상태 보통. 설명대로.",          "3"},
            new String[]{"새 제품처럼 깔끔히 포장되어 있었음.", "5"},
            new String[]{"당일 발송. 다음날 수령.",            "5"},
            new String[]{"주문 후 5일 지나서야 발송. 실망.",   "2"}
        );

        for (String[] r : reviews) {
            String id   = UUID.randomUUID().toString();
            String text = "별점: " + r[1] + "점. 후기: " + r[0];
            vectorStore.add(List.of(new Document(id, text,
                    Map.of("sellerId", SELLER_A, "score", Integer.parseInt(r[1])))));
            insertedIds.add(id);
        }

        String userQuery = "이 판매자 배송 빠른가요?";
        String filter    = "sellerId == " + SELLER_A;

        List<Document> without = search(userQuery, filter, 10);

        String hydeQuery = chatModel.call(HYDE_PROMPT.formatted(userQuery));
        String safeHyde  = hydeQuery != null ? hydeQuery : userQuery;
        List<Document> with = search(safeHyde, filter, 10);

        sep("유사도 상세 — 질문: \"" + userQuery + "\"");

        System.out.println("  ── HyDE 미적용 ──────────────────────────────────────────────────────");
        for (int i = 0; i < without.size(); i++) {
            Document d  = without.get(i);
            boolean hit = isDeliveryPositive(d.getText());
            System.out.printf("  %2d위 [%.4f]  %s%s%n",
                    i + 1, simScore(d), d.getText(), hit ? "  ← ✓ 배송긍정" : "");
        }

        System.out.println();
        System.out.println("  ── HyDE 적용 ────────────────────────────────────────────────────────");
        System.out.println("  변환된 검색어:");
        for (String line : safeHyde.strip().split("\n")) {
            if (!line.isBlank()) System.out.println("    " + line);
        }
        System.out.println();
        for (int i = 0; i < with.size(); i++) {
            Document d  = with.get(i);
            boolean hit = isDeliveryPositive(d.getText());
            System.out.printf("  %2d위 [%.4f]  %s%s%n",
                    i + 1, simScore(d), d.getText(), hit ? "  ← ✓ 배송긍정" : "");
        }

        System.out.println();
        System.out.println("  ── 서비스 경유 (cleanHydeResult + score >= 4 필터) ───────────────────");

        List<String> withService = reviewEmbeddingService.search(SELLER_A, userQuery);
        if (withService.isEmpty()) {
            System.out.println("  (결과 없음 — 유사도 0.4 미만 또는 score >= 4 후기 부재)");
        } else {
            for (int i = 0; i < withService.size(); i++) {
                boolean hit = isDeliveryPositive(withService.get(i));
                System.out.printf("  %2d위  %s%s%n", i + 1, withService.get(i), hit ? "  ← ✓ 배송긍정" : "");
            }
        }

        double rankWithout  = avgRankOf(without, t -> isDeliveryPositive(t));
        double rankWith     = avgRankOf(with,    t -> isDeliveryPositive(t));
        double rankService  = avgRankOfTexts(withService, this::isDeliveryPositive);

        System.out.println();
        System.out.printf("  ▶ '✓ 배송긍정' 평균 순위 — 미적용: %.1f위  |  HyDE(raw): %.1f위  |  서비스: %.1f위%n",
                rankWithout, rankWith, rankService);
        System.out.println("    (숫자 작을수록 상위 / 서비스 경로에서 ★1 후기가 사라지면 필터 정상 동작)");
        sep(null);
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────

    private List<Document> search(String query, String filter, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query).topK(topK)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build()
        );
    }

    private double precision(List<Document> results, String relevantTopic, List<LabeledReview> dataset) {
        if (results.isEmpty()) return 0.0;
        long hits = results.stream()
                .filter(d -> isMatchingTopic(d.getText(), relevantTopic, dataset))
                .count();
        return (double) hits / results.size();
    }

    private boolean isMatchingTopic(String docText, String topic, List<LabeledReview> dataset) {
        String stripped = stripPrefix(docText);
        return dataset.stream()
                .filter(r -> r.topic().equals(topic))
                .anyMatch(r -> stripped.contains(r.text()) || r.text().contains(stripped.trim()));
    }

    private boolean isDeliveryPositive(String text) {
        return text.contains("빠름") || text.contains("빠른") || text.contains("당일") || text.contains("다음날");
    }

    private double avgRankOfTexts(List<String> results, Predicate<String> filter) {
        double sum = 0;
        int    cnt = 0;
        for (int i = 0; i < results.size(); i++) {
            if (filter.test(results.get(i))) {
                sum += (i + 1);
                cnt++;
            }
        }
        return cnt == 0 ? results.size() + 1 : sum / cnt;
    }

    private double avgRankOf(List<Document> results, Predicate<String> filter) {
        double sum = 0;
        int    cnt = 0;
        for (int i = 0; i < results.size(); i++) {
            if (filter.test(results.get(i).getText())) {
                sum += (i + 1);
                cnt++;
            }
        }
        return cnt == 0 ? results.size() + 1 : sum / cnt;
    }

    private int score(Document d) {
        Object s = d.getMetadata().get("score");
        return s instanceof Number n ? n.intValue() : 3;
    }

    private double simScore(Document d) {
        return d.getScore() != null ? d.getScore() : 0.0;
    }

    private String stripPrefix(String text) {
        return text.replaceAll("^별점: \\d+점\\. 후기: ", "");
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

/*
 ● ---
  Test 1 — HyDE 변환 결과 육안 확인

  유저 질문 → chatModel.call(HYDE_PROMPT) → 출력 표시

  DB나 pgvector 없음. LLM 호출만 해서 질문이 어떤 후기 문체로 변환되는지 눈으로 확인하는 용도야. 긍정/부정 방향성이 프롬프트 개선 후 올바르게 반영되는지 보는 테스트.

  ---
  Test 2 — Precision@3 정량 비교

  20개 레이블 데이터 → pgvector 저장
  4개 쿼리 × (HyDE 미적용 / HyDE 적용) 각각 검색
  상위 3개 중 relevantTopic 일치 비율 = Precision@3

  LabeledReview에 topic 태그(delivery_pos, delivery_neg, item, trust)를 붙여두고, 검색 결과 상위 3개 중 해당 topic 후기가 몇 개인지 측정해. HyDE가 숫자로 얼마나 효과 있는지 보는 테스트. 테스트 끝나면 @AfterEach로 삽입한 데이터 전부 삭제.

  ---
  Test 3 — Contextual Retrieval 효과 비교

  SELLER_A: "별점: N점. 후기: 텍스트"  (별점 포함)
  SELLER_B: "텍스트"만                  (별점 미포함)
  동일한 짧고 중립적인 후기 16개를 각각 저장

  핵심 설계는 텍스트가 "받았습니다", "배송 완료" 같이 중립이라 별점 없이는 긍/부정 구분 불가한 데이터를 씀. 쿼리는 두 종류:

  - 감성 쿼리 ("정말 만족스러운 거래였나요?") → 텍스트 의미로만 탐색, 별점 효과 미미
  - 별점 직접 언급 쿼리 ("별점 5점짜리 후기가 있나요?") → SELLER_A의 "별점: 5점" 텍스트와 직접 매칭 → 극적 차이

  검색 후 상위 K개의 평균 별점, 고점수/저점수 개수를 비교 출력해.

  ---
  Test 4 — 유사도 상세 비교 (3-way)

  10개 후기 → pgvector 저장
  동일 쿼리로 3가지 경로 검색:

    ① HyDE 미적용: 원본 쿼리 → vectorStore 직접
    ② HyDE 적용(raw): chatModel.call(HYDE_PROMPT) 결과 → vectorStore 직접
    ③ 서비스 경유: reviewEmbeddingService.search()
                    └→ JSON 파싱 or cleanHydeResult + score >= 4 필터

  ①②는 filterExpression에 score 조건 없이 유사도만으로 검색 → 임베딩 모델 한계 그대로 노출.
  ③은 프로덕션 search()를 그대로 타서 score 필터 적용 → ★1~★2 후기 사전 제외.

  각 경로별 전체 순위 + 배송긍정 후기 평균 순위를 나란히 출력해서 세 방식의 차이를 한눈에 볼 수 있어.
*/