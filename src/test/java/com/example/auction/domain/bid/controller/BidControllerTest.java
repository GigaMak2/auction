package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.service.BidCommandFacade;
import com.example.auction.domain.bid.service.BidQueryService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BidController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BidCommandFacade commandService;

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
    // 입찰 생성 v2 (분산락)
    // ========================

    @Test
    @DisplayName("입찰 생성 성공")
    void placeBid_success() throws Exception {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(5000), "열심히 입찰합니다");

        Bid bid = mock(Bid.class);
        given(bid.getId()).willReturn(1L);
        given(bid.getAuctionId()).willReturn(1L);
        given(bid.getPrice()).willReturn(BigDecimal.valueOf(5000));
        given(bid.getDescription()).willReturn("열심히 입찰합니다");
        given(bid.getCreatedAt()).willReturn(LocalDateTime.now());
        given(bid.getStatus()).willReturn(BidAuctionStatus.ACTIVE);
        BidResponse response = BidResponse.of(bid);

        given(commandService.placeBidDis(any(), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auctions/{auctionId}/bids/v2", 1L)
                        .header("Authorization", "Bearer accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰이 완료되었습니다"))
                .andDo(document("bid/place-bid",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자")),
                        requestFields(
                                fieldWithPath("price").description("입찰 금액 (0 초과 정수)"),
                                fieldWithPath("description").description("입찰 설명 (1024자 이하)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("입찰 생성 실패 - 금액 null")
    void placeBid_fail_priceNull() throws Exception {
        // given
        BidRequest request = new BidRequest(null, "입찰합니다");

        // when & then
        mockMvc.perform(post("/api/auctions/1/bids/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입찰 금액은 필수입니다"));
    }

    @Test
    @DisplayName("입찰 생성 실패 - 금액 0 이하")
    void placeBid_fail_priceNotPositive() throws Exception {
        // given
        BidRequest request = new BidRequest(BigDecimal.ZERO, "입찰합니다");

        // when & then
        mockMvc.perform(post("/api/auctions/1/bids/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입찰 금액은 0보다 커야 합니다"));
    }

    @Test
    @DisplayName("입찰 생성 실패 - 금액 소수점 포함")
    void placeBid_fail_priceHasFraction() throws Exception {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(5000.5), "입찰합니다");

        // when & then
        mockMvc.perform(post("/api/auctions/1/bids/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("입찰 금액은 정수여야 합니다"));
    }

    @Test
    @DisplayName("입찰 생성 실패 - 설명 1024자 초과")
    void placeBid_fail_descriptionTooLong() throws Exception {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(5000), "a".repeat(1025));

        // when & then
        mockMvc.perform(post("/api/auctions/1/bids/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("설명이 너무 깁니다"));
    }


    // ========================
    // 특정 경매 입찰 조회
    // ========================

    @Test
    @DisplayName("입찰 목록 조회 성공")
    void getBids_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<BidListResponse> response = new PageResponse<>(
                List.of(
                        new BidListResponse(1L, 1L, BigDecimal.valueOf(3000), now),
                        new BidListResponse(2L, 1L, BigDecimal.valueOf(4000), now)
                ), 0, 1, 2L, 20, true);

        given(queryService.getBids(any(), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auctions/{auctionId}/bids/v1", 1L)
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰 내역 조회가 완료되었습니다"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("bid/get-bids",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (0 ~ 100)").optional()
                        )

                ));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - page 음수")
    void getBids_fail_pageIsNegative() throws Exception {
        mockMvc.perform(get("/api/auctions/1/bids/v1").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - size 0")
    void getBids_fail_sizeIsZero() throws Exception {
        mockMvc.perform(get("/api/auctions/1/bids/v1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("입찰 목록 조회 실패 - size 100 초과")
    void getBids_fail_sizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/auctions/1/bids/v1").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 입찰 결과 조회 (낙찰)
    // ========================

    @Test
    @DisplayName("입찰 결과 조회 성공")
    void getWinnerBid_success() throws Exception {
        // given
        Bid bid = mock(Bid.class);
        given(bid.getId()).willReturn(1L);
        given(bid.getAuctionId()).willReturn(1L);
        given(bid.getPrice()).willReturn(BigDecimal.valueOf(5000));
        given(bid.getDescription()).willReturn("열심히 입찰합니다");
        given(bid.getCreatedAt()).willReturn(LocalDateTime.now());
        given(bid.getStatus()).willReturn(BidAuctionStatus.ACTIVE);
        BidResponse response = BidResponse.of(bid);

        given(queryService.getWinnerBid(any(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auctions/{auctionId}/bids/winner/v1", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("입찰 결과 조회가 완료되었습니다"))
                .andDo(document("bid/get-winner-bid",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자"))
                ));
    }


    // ========================
    // 현재 최저가 입찰 조회
    // ========================
    @Test
    @DisplayName("현재 최저가 입찰 조회 성공")
    void getCurrentMinBid_success() throws Exception {
        // given
        Bid bid = mock(Bid.class);
        given(bid.getId()).willReturn(1L);
        given(bid.getAuctionId()).willReturn(1L);
        given(bid.getPrice()).willReturn(BigDecimal.valueOf(5000));
        given(bid.getDescription()).willReturn("열심히 입찰합니다");
        given(bid.getCreatedAt()).willReturn(LocalDateTime.now());
        given(bid.getStatus()).willReturn(BidAuctionStatus.ACTIVE);
        BidResponse response = BidResponse.of(bid);

        given(queryService.getCurrentMinBid(any(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auctions/{auctionId}/bids/current/v1", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("현재 최저가 입찰 조회가 완료되었습니다"))
                .andDo(document("bid/get-current-min-bid",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자"))
                ));
    }
}