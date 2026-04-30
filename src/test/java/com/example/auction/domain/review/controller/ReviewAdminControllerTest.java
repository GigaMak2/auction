package com.example.auction.domain.review.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.service.ReviewAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReviewAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ReviewAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewAdminService reviewAdminService;

    @BeforeEach
    void setUpSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========================
    // 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("리뷰 목록 조회 성공")
    void getReviewList_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<ReviewAdminListResponse> response = new PageResponse<>(
                List.of(
                        new ReviewAdminListResponse(1L, 10L, 1L, 2L, 5, now),
                        new ReviewAdminListResponse(2L, 11L, 3L, 4L, 3, now)
                ), 0, 1, 2L, 20, true);

        given(reviewAdminService.getReviewList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - page 음수")
    void getReviewList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - size 0")
    void getReviewList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/reviews")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - size 100 초과")
    void getReviewList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/reviews")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 리뷰 강제 삭제
    // ========================

    @Test
    @DisplayName("리뷰 강제 삭제 성공")
    void forceDelete_success() throws Exception {
        // given
        doNothing().when(reviewAdminService).forceDelete(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 강제 삭제 요청 성공"));

        verify(reviewAdminService).forceDelete(1L);
    }
}