package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.service.BidAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BidAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class BidAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidAdminService bidAdminService;

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
    // 입찰 목록 조회
    // ========================

    @Test
    @DisplayName("입찰 목록 조회 성공")
    void getBidList_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<BidAdminListResponse> response = new PageResponse<>(
                List.of(
                        new BidAdminListResponse(1L, 10L, 1L, BigDecimal.valueOf(1000), BidAuctionStatus.ACTIVE, now),
                        new BidAdminListResponse(2L, 10L, 2L, BigDecimal.valueOf(900), BidAuctionStatus.CLOSED, now),
                        new BidAdminListResponse(3L, 10L, 3L, BigDecimal.valueOf(800), BidAuctionStatus.CANCELLED, now)
                ), 0, 1, 3L, 20, true);

        given(bidAdminService.getBidList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/bids")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andDo(document("bid-admin/get-bid-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (1 ~ 100)").optional(),
                                parameterWithName("status").description("입찰 상태 필터 (ACTIVE / CLOSED / CANCELLED)").optional(),
                                parameterWithName("auctionId").description("경매 식별자 필터").optional(),
                                parameterWithName("userId").description("사용자 식별자 필터").optional()
                        )
                ));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - page 음수")
    void getBidList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/bids")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - size 0")
    void getBidList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/bids")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - size 100 초과")
    void getBidList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/bids")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 입찰 강제 취소
    // ========================

    @Test
    @DisplayName("입찰 강제 취소 성공")
    void forceCancel_success() throws Exception {
        // given
        doNothing().when(bidAdminService).forceCancel(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/bids/{bidId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰을 강제 취소했습니다"))
                .andDo(document("bid-admin/force-cancel",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        pathParameters(parameterWithName("bidId").description("입찰 식별자"))
                ));

        verify(bidAdminService).forceCancel(1L);
    }
}