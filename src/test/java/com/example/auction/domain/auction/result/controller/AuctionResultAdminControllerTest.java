package com.example.auction.domain.auction.result.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.auction.result.dto.AuctionResultAdminListResponse;
import com.example.auction.domain.auction.result.service.AuctionResultAdminService;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuctionResultAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AuctionResultAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionResultAdminService auctionResultAdminService;

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
    // 경매 결과 목록 조회
    // ========================

    @Test
    @DisplayName("경매 결과 목록 조회 성공")
    void getAuctionResultList_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<AuctionResultAdminListResponse> response = new PageResponse<>(
                List.of(
                        new AuctionResultAdminListResponse(1L, 1L, 1L, 1L, 2L, BigDecimal.valueOf(10000), now),
                        new AuctionResultAdminListResponse(2L, 2L, 3L, 2L, 4L, BigDecimal.valueOf(20000), now)
                ), 0, 1, 2L, 20, true);

        given(auctionResultAdminService.getAuctionResultList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/auction-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매 결과 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("auction-result-admin/get-auction-result-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("경매 결과 목록 조회 실패 - page 음수")
    void getAuctionResultList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auction-results")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 결과 목록 조회 실패 - size 0")
    void getAuctionResultList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auction-results")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 결과 목록 조회 실패 - size 100 초과")
    void getAuctionResultList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/auction-results")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }
}