package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.service.BidQueryService;
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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BidUserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class BidUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidQueryService queryService;

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
    // 내 입찰 조회
    // ========================

    @Test
    @DisplayName("내 입찰 조회 성공")
    void getMyBids_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<BidListResponse> response = new PageResponse<>(
                List.of(
                        new BidListResponse(1L, 10L, BigDecimal.valueOf(3000), now),
                        new BidListResponse(2L, 11L, BigDecimal.valueOf(5000), now)
                ), 0, 1, 2L, 20, true);

        given(queryService.getMyBids(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/me/bids")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 입찰 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("bid-user/get-my-bids",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (0 ~ 100)").optional()
                        )
                ));;
    }

    @Test
    @DisplayName("내 입찰 조회 성공 - 빈 목록")
    void getMyBids_success_empty() throws Exception {
        // given
        PageResponse<BidListResponse> response = new PageResponse<>(
                List.of(), 0, 0, 0L, 20, true);

        given(queryService.getMyBids(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/me/bids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}