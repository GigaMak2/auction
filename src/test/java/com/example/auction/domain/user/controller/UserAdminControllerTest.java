package com.example.auction.domain.user.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.user.dto.UserDetailGetResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.enums.AuthProvider;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.service.UserAdminService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

@WebMvcTest({UserAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAdminService userAdminService;

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
    // 사용자 목록 조회
    // ========================

    @Test
    @DisplayName("사용자 목록 조회 성공")
    void getUserList_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PageResponse<UserListGetResponse> response = new PageResponse<>(
                List.of(
                        new UserListGetResponse(1L, "user1@test.com", UserRole.USER, false, now, null),
                        new UserListGetResponse(2L, "user2@test.com", UserRole.USER, false, now, null)
                ), 0, 1, 2L, 20, true);

        given(userAdminService.getUserList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer accessToken")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용자 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("user-admin/get-user-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0 이상)").optional(),
                                parameterWithName("size").description("페이지 크기 (1 ~ 100)").optional(),
                                parameterWithName("deleted").description("탈퇴 여부 필터 (true / false)").optional(),
                                parameterWithName("role").description("권한 필터 (USER / ADMIN)").optional(),
                                parameterWithName("email").description("이메일 검색").optional()
                        )
                ));
    }

    @Test
    @DisplayName("사용자 목록 조회 실패 - page 음수")
    void getUserList_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("사용자 목록 조회 실패 - size 0")
    void getUserList_fail_sizeIsZero() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/users")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("사용자 목록 조회 실패 - size 100 초과")
    void getUserList_fail_sizeTooLarge() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/users")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========================
    // 사용자 상세 조회
    // ========================

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUserDetail_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        UserDetailGetResponse response = new UserDetailGetResponse(
                1L, "user@test.com", null, UserRole.USER, AuthProvider.KAKAO, false, now, null);

        given(userAdminService.getUserDetail(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/users/{userId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용자 상세 조회 요청 성공"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andDo(document("user-admin/get-user-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        pathParameters(parameterWithName("userId").description("사용자 식별자"))
                ));
    }


    // ========================
    // 사용자 강제 탈퇴
    // ========================

    @Test
    @DisplayName("사용자 강제 탈퇴 성공")
    void forceWithdraw_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        UserWithdrawResponse response = new UserWithdrawResponse(
                1L, "user@test.com", UserRole.USER, now, now);

        given(userAdminService.forceWithdraw(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/admin/users/{userId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용자 강제 탈퇴 요청 성공"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andDo(document("user-admin/force-withdraw",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN)")),
                        pathParameters(parameterWithName("userId").description("사용자 식별자"))
                ));
    }
}