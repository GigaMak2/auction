package com.example.auction.domain.auth.controller;

import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.auth.dto.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.service.AuthAdminService;
import com.example.auction.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AuthAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthAdminService authAdminService;


    // ========================
    // 관리자 회원가입
    // ========================

    @Test
    @DisplayName("관리자 회원가입 성공")
    void signup_success() throws Exception {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", "secret-key");
        AuthSignupResponse response = new AuthSignupResponse(1L, "admin@test.com", UserRole.ADMIN, LocalDateTime.now());

        given(authAdminService.signup(any(AuthAdminSignupRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관리자 회원가입했습니다"))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andDo(document("auth-admin/signup",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("password").description("비밀번호 (8자 이상)"),
                                fieldWithPath("adminSecretKey").description("관리자 인증 키")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 이메일 형식 불일치")
    void signup_fail_invalidEmail() throws Exception {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("adminEmail", "password123", "secret-key");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 비밀번호 8자 미만")
    void signup_fail_shortPassword() throws Exception {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "pw123", "secret-key");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이어야 합니다"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 관리자 인증 키 공백")
    void signup_fail_blankAdminSecretKey() throws Exception {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", "");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("관리자 인증 키를 입력해주세요"));
    }
}