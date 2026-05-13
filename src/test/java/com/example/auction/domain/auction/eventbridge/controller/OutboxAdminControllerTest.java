package com.example.auction.domain.auction.eventbridge.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.auction.eventbridge.dto.OutboxAdminResponse;
import com.example.auction.domain.auction.eventbridge.service.OutboxAdminService;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OutboxAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class OutboxAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutboxAdminService outboxAdminService;

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
    // FAILED outbox 조회
    // ========================

    @Test
    @DisplayName("FAILED outbox 조회 성공")
    void getFailedOutbox_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<OutboxAdminResponse> response = List.of(
                new OutboxAdminResponse(1L, 1L, "START", now.plusHours(1), "FAILED", 3, now, now),
                new OutboxAdminResponse(2L, 2L, "END", now.plusHours(2), "FAILED", 3, now, now)
        );
        given(outboxAdminService.getFailedOutbox()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/outbox")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("FAILED outbox 조회에 성공했습니다"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andDo(document("outbox-admin/get-failed-outbox",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].id").description("outbox ID"),
                                fieldWithPath("data[].auctionId").description("경매 ID"),
                                fieldWithPath("data[].eventType").description("이벤트 타입 (START / END)"),
                                fieldWithPath("data[].scheduledAt").description("스케줄 등록 목표 시각"),
                                fieldWithPath("data[].status").description("outbox 상태 (FAILED)"),
                                fieldWithPath("data[].attemptCount").description("시도 횟수"),
                                fieldWithPath("data[].lastAttemptedAt").description("마지막 시도 시각"),
                                fieldWithPath("data[].createdAt").description("생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("FAILED outbox 조회 성공 - 빈 리스트")
    void getFailedOutbox_success_empty() throws Exception {
        // given
        given(outboxAdminService.getFailedOutbox()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/admin/outbox")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ========================
    // outbox 재시도
    // ========================

    @Test
    @DisplayName("outbox 재시도 성공")
    void retry_success() throws Exception {
        // given
        willDoNothing().given(outboxAdminService).retry(anyLong());

        // when & then
        mockMvc.perform(post("/api/admin/outbox/{outboxId}/retry", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("재시도 요청을 성공했습니다"))
                .andDo(document("outbox-admin/retry-outbox",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        pathParameters(
                                parameterWithName("outboxId").description("재시도할 outbox ID")
                        )
                ));
    }

    @Test
    @DisplayName("outbox 재시도 실패 - outbox 없음")
    void retry_fail_notFound() throws Exception {
        // given
        willThrow(new IllegalArgumentException("outbox not found: 999"))
                .given(outboxAdminService).retry(999L);

        // when & then
        mockMvc.perform(post("/api/admin/outbox/{outboxId}/retry", 999L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
