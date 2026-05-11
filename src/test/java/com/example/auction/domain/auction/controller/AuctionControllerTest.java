package com.example.auction.domain.auction.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.service.AuctionService;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuctionController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionService auctionService;

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
    // 경매 단건 조회
    // ========================

    @Test
    @DisplayName("경매 단건 조회 성공")
    void getAuction_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        GetAuctionResponse response = new GetAuctionResponse(
                1L, 1L, "좋은 상품입니다", BigDecimal.valueOf(50000), "맥북 프로",
                AuctionStatus.ACTIVE, now, now.plusDays(1), null, 1L, now);

        given(auctionService.getAuction(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auctions/{auctionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매를 조회했습니다"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.itemName").value("맥북 프로"))
                .andDo(document("auction/get-auction",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자"))
                ));
    }


    // ========================
    // 경매 전체 조회 (공개)
    // ========================

    @Test
    @DisplayName("경매 전체 조회 성공")
    void getManyAuctionsPublic_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<GetManyAuctionsResponse> response = new PageResponse<>(
                List.of(
                        new GetManyAuctionsResponse(1L, 1L, BigDecimal.valueOf(50000), "맥북 프로", AuctionStatus.ACTIVE, now, now.plusDays(1), null, 1L, now),
                        new GetManyAuctionsResponse(2L, 2L, BigDecimal.valueOf(30000), "아이패드", AuctionStatus.READY, now, now.plusDays(2), null, 2L, now)
                ), 0, 1, 2L, 10, true);

        given(auctionService.getManyAuctionsPublic(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auctions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("auction/get-many-auctions-public",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드").optional(),
                                parameterWithName("maxPriceMin").description("최소 금액 (0 이상)").optional(),
                                parameterWithName("maxPriceMax").description("최대 금액 (0 초과)").optional(),
                                parameterWithName("status").description("경매 상태 필터 (READY / ACTIVE / CLOSED / CANCELLED)").optional(),
                                parameterWithName("categoryId").description("카테고리 식별자 (1 이상)").optional(),
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (1 ~ 100)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("경매 전체 조회 실패 - page 음수")
    void getManyAuctionsPublic_fail_pageIsNegative() throws Exception {
        mockMvc.perform(get("/api/auctions").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 전체 조회 실패 - size 0")
    void getManyAuctionsPublic_fail_sizeIsZero() throws Exception {
        mockMvc.perform(get("/api/auctions").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("경매 전체 조회 실패 - size 100 초과")
    void getManyAuctionsPublic_fail_sizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/auctions").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 내 경매 목록 조회
    // ========================

    @Test
    @DisplayName("내 경매 목록 조회 성공")
    void getManyAuctionsMe_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<GetManyAuctionsResponse> response = new PageResponse<>(
                List.of(
                        new GetManyAuctionsResponse(1L, 1L, BigDecimal.valueOf(50000), "맥북 프로", AuctionStatus.ACTIVE, now, now.plusDays(1), null, 1L, now)
                ), 0, 1, 1L, 10, true);

        given(auctionService.getManyAuctionsMe(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/me/auctions")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 경매 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andDo(document("auction/get-many-auctions-me",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드").optional(),
                                parameterWithName("maxPriceMin").description("최소 금액 (0 이상)").optional(),
                                parameterWithName("maxPriceMax").description("최대 금액 (0 초과)").optional(),
                                parameterWithName("status").description("경매 상태 필터 (READY / ACTIVE / CLOSED / CANCELLED)").optional(),
                                parameterWithName("categoryId").description("카테고리 식별자 (1 이상)").optional(),
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (1 ~ 100)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("내 경매 목록 조회 실패 - page 음수")
    void getManyAuctionsMe_fail_pageIsNegative() throws Exception {
        mockMvc.perform(get("/api/me/auctions").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }


    // ========================
    // 경매 생성
    // ========================

    @Test
    @DisplayName("경매 생성 성공")
    void createAuction_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setItemName("맥북 프로");
        request.setDescription("좋은 상품입니다");
        request.setMaxPrice(BigDecimal.valueOf(50000));
        request.setCategoryId(1L);
        request.setStartedAt(now.plusHours(1));
        request.setEndedAt(now.plusDays(1));

        GetAuctionResponse response = new GetAuctionResponse(
                1L, 1L, "좋은 상품입니다", BigDecimal.valueOf(50000), "맥북 프로",
                AuctionStatus.READY, now.plusHours(1), now.plusDays(1), null, 1L, now);

        given(auctionService.createAuction(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auctions")
                        .header("Authorization", "Bearer accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("경매를 생성했습니다"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.itemName").value("맥북 프로"))
                .andDo(document("auction/create-auction",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        requestFields(
                                fieldWithPath("itemName").description("상품명 (256자 이하)"),
                                fieldWithPath("description").description("상품 설명 (1024자 이하)").optional(),
                                fieldWithPath("maxPrice").description("최고 가격 (0 초과)"),
                                fieldWithPath("categoryId").description("카테고리 식별자 (1 이상)"),
                                fieldWithPath("startedAt").description("경매 시작일"),
                                fieldWithPath("endedAt").description("경매 종료일")
                        )
                ));
    }

    @Test
    @DisplayName("경매 생성 실패 - 상품명 없음")
    void createAuction_fail_itemNameBlank() throws Exception {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setItemName("");
        request.setMaxPrice(BigDecimal.valueOf(50000));
        request.setCategoryId(1L);
        request.setStartedAt(LocalDateTime.now().plusHours(1));
        request.setEndedAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("경매 상품 이름은 필수입니다"));
    }

    @Test
    @DisplayName("경매 생성 실패 - 상품명 256자 초과")
    void createAuction_fail_itemNameTooLong() throws Exception {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setItemName("a".repeat(257));
        request.setMaxPrice(BigDecimal.valueOf(50000));
        request.setCategoryId(1L);
        request.setStartedAt(LocalDateTime.now().plusHours(1));
        request.setEndedAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("경매 상품 이름이 너무 깁니다"));
    }

    @Test
    @DisplayName("경매 생성 실패 - 최고가 0 이하")
    void createAuction_fail_maxPriceNotPositive() throws Exception {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setItemName("맥북 프로");
        request.setMaxPrice(BigDecimal.ZERO);
        request.setCategoryId(1L);
        request.setStartedAt(LocalDateTime.now().plusHours(1));
        request.setEndedAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("경매 상품의 가격은 0보다 커야합니다"));
    }


    // ========================
    // 경매 취소
    // ========================

    @Test
    @DisplayName("경매 취소 성공")
    void cancelAuction_success() throws Exception {
        doNothing().when(auctionService).cancelAuction(eq(1L), any());

        mockMvc.perform(delete("/api/auctions/{auctionId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isNoContent())
                .andDo(document("auction/cancel-auction",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("auctionId").description("경매 식별자"))
                ));
    }
}