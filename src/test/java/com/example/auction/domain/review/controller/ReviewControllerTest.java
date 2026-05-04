package com.example.auction.domain.review.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.service.ReviewImageService;
import com.example.auction.domain.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReviewController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReviewImageService reviewImageService;

    @BeforeEach
    void setUpSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========================
    // 리뷰 생성
    // ========================

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(10L, 5, "좋아요", null);
        ReviewCreateResponse response = new ReviewCreateResponse(1L, 10L, 1L, 2L, 5, "좋아요", null, LocalDateTime.now());

        given(reviewService.createReview(eq(1L), any(ReviewCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 생성 요청 성공"))
                .andExpect(jsonPath("$.data.auctionId").value(10L))
                .andExpect(jsonPath("$.data.score").value(5))
                .andDo(document("review/create-review",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - auctionId null")
    void createReview_fail_auctionIdNull() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(null, 5, "좋아요", null);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("경매 식별자를 입력해주세요"));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - score 1 미만")
    void createReview_fail_scoreLessThan1() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(10L, 0, "좋아요", null);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("별점은 최소 1점입니다"));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - score 5 초과")
    void createReview_fail_scoreGreaterThan5() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(10L, 6, "좋아요", null);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("별점은 최대 5점입니다"));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 설명 500자 초과")
    void createReview_fail_descriptionTooLong() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(1L, 5, "a".repeat(501), null);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리뷰 내용은 500자 이하로 입력해주세요"));
    }


    // ========================
    // 작성한 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("작성한 리뷰 목록 조회 성공")
    void getWrittenReviewList_success() throws Exception {
        // given
        PageResponse<ReviewListGetResponse> response = new PageResponse<>(
                List.of(
                        new ReviewListGetResponse(1L, 10L, 2L, LocalDateTime.now(), LocalDateTime.now(), null),
                        new ReviewListGetResponse(2L, 11L, 3L, LocalDateTime.now(), LocalDateTime.now(), null)
                ), 0, 1, 2L, 10, true);

        given(reviewService.getWrittenReviewList(eq(1L), any(ReviewSearchCondition.class))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("작성한 리뷰 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("review/get-written-review-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)"),
                                parameterWithName("size").description("페이지 크기 (0 ~ 100)"),
                                parameterWithName("startDate").description("조회 시작일 (yyyy-MM-dd)").optional(),
                                parameterWithName("endDate").description("조회 종료일 (yyyy-MM-dd)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 실패 - page 음수")
    void getWrittenReviewList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 실패 - size 0")
    void getWrittenReviewList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 실패 - size 100 초과")
    void getWrittenReviewList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 실패 - startDate가 endDate 이후")
    void getWrittenReviewList_fail_invalidDateRange() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("시작일은 종료일보다 이후일 수 없습니다"));
    }


    // ========================
    // 받은 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("받은 리뷰 목록 조회 성공")
    void getReceivedReviewList_success() throws Exception {
        // given
        PageResponse<ReviewListGetResponse> response = new PageResponse<>(
                List.of(
                        new ReviewListGetResponse(1L, 10L, 2L, LocalDateTime.now(), LocalDateTime.now(), null),
                        new ReviewListGetResponse(2L, 11L, 3L, LocalDateTime.now(), LocalDateTime.now(), null)
                ), 0, 1, 2L, 10, true);

        given(reviewService.getReceivedReviewList(eq(1L), any(ReviewSearchCondition.class))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("받은 리뷰 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("review/get-received-review-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)"),
                                parameterWithName("size").description("페이지 크기 (0 ~ 100)"),
                                parameterWithName("startDate").description("조회 시작일 (yyyy-MM-dd)").optional(),
                                parameterWithName("endDate").description("조회 종료일 (yyyy-MM-dd)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 실패 - page 음수")
    void getReceivedReviewList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 실패 - size 0")
    void getReceivedReviewList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 실패 - size 100 초과")
    void getReceivedReviewList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 실패 - startDate가 endDate 이후")
    void getReceivedReviewList_fail_invalidDateRange() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("시작일은 종료일보다 이후일 수 없습니다"));
    }


    // ========================
    // 리뷰 상세 조회
    // ========================

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    void getReview_success() throws Exception {
        // given
        ReviewGetResponse response = new ReviewGetResponse(
                1L, 10L, 1L, 2L, 5, "좋아요", null, LocalDateTime.now(), LocalDateTime.now());

        given(reviewService.getReview(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/reviews/{reviewId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 상세 조회 요청 성공"))
                .andExpect(jsonPath("$.data.reviewId").value(1L))
                .andExpect(jsonPath("$.data.score").value(5))
                .andDo(document("review/get-review",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("reviewId").description("리뷰 식별자"))
                ));
    }


    // ========================
    // 리뷰 수정
    // ========================

    @Test
    @DisplayName("리뷰 수정 성공")
    void modifyReview_success() throws Exception {
        // given
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요", null);
        ReviewModifyResponse response = new ReviewModifyResponse(1L, 1, "별로에요", null, LocalDateTime.now(), LocalDateTime.now());

        given(reviewService.modifyReview(eq(1L), eq(1L), any(ReviewModifyRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", 1L)
                        .header("Authorization", "Bearer accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 수정 요청 성공"))
                .andExpect(jsonPath("$.data.reviewId").value(1L))
                .andExpect(jsonPath("$.data.score").value(1))
                .andDo(document("review/modify-review",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("reviewId").description("리뷰 식별자"))
                ));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - score 1 미만")
    void modifyReview_fail_scoreLessThan1() throws Exception {
        // given
        ReviewModifyRequest request = new ReviewModifyRequest(0, "친절해요", null);

        // when & then
        mockMvc.perform(patch("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("별점은 최소 1점입니다"));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - score 5 초과")
    void modifyReview_fail_scoreGreaterThan5() throws Exception {
        // given
        ReviewModifyRequest request = new ReviewModifyRequest(6, "친절해요", null);

        // when & then
        mockMvc.perform(patch("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("별점은 최대 5점입니다"));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 설명 500자 초과")
    void modifyReview_fail_descriptionTooLong() throws Exception {
        // given
        ReviewModifyRequest request = new ReviewModifyRequest(null, "a".repeat(501), null);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리뷰 내용은 500자 이하로 입력해주세요"));
    }


    // ========================
    // 리뷰 삭제
    // ========================

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() throws Exception {
        // given
        doNothing().when(reviewService).deleteReview(1L, 1L);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 삭제 요청 성공"))
                .andDo(document("review/delete-review",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("reviewId").description("리뷰 식별자"))
                ));

        verify(reviewService).deleteReview(1L, 1L);
    }
}

