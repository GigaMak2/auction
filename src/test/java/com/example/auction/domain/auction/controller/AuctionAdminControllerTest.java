package com.example.auction.domain.auction.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.auction.dto.response.AuctionAdminListResponse;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.service.AuctionAdminService;
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

@WebMvcTest({AuctionAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AuctionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionAdminService auctionAdminService;

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
    // 경매 목록 조회
    // ========================

    @Test
    @DisplayName("경매 목록 조회 성공")
    void getAuctionList_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<AuctionAdminListResponse> response = new PageResponse<>(
                List.of(
                        new AuctionAdminListResponse(1L, 1L, "상품1", 1L, AuctionStatus.ACTIVE, now, now, now.plusDays(1), null),
                        new AuctionAdminListResponse(2L, 2L, "상품2", 2L, AuctionStatus.READY, now, now, now.plusDays(1), null),
                        new AuctionAdminListResponse(3L, 3L, "상품3", 3L, AuctionStatus.CANCELLED, now, now, now.plusDays(1), now.plusMinutes(30))

                ), 0, 1, 3L, 20, true);

        given(auctionAdminService.getAuctionList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/auctions")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andDo(document("auction-admin/get-auction-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (1 ~ 100)").optional(),
                                parameterWithName("status").description("경매 상태 필터 (READY / ACTIVE / CLOSED / CANCELLED)").optional(),
                                parameterWithName("keyword").description("검색 키워드").optional()
                        )
                ));;
    }

    @Test
    @DisplayName("경매 목록 조회 실패 - page 음수")
    void getAuctionList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auctions")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 목록 조회 실패 - size 0")
    void getAuctionList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auctions")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 목록 조회 실패 - size 100 초과")
    void getAuctionList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auctions")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 경매 강제 취소
    // ========================

    @Test
    @DisplayName("경매 강제 취소 성공")
    void forceCancel_success() throws Exception {
        // given
        doNothing().when(auctionAdminService).forceCancel(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/auctions/{auctionId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매를 강제 취소했습니다"))
                .andDo(document("auction-admin/force-cancel",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자"))
                ));

        verify(auctionAdminService).forceCancel(1L);
    }
}