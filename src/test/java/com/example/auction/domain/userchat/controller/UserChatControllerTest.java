package com.example.auction.domain.userchat.controller;

import com.example.auction.common.config.security.CustomAccessDeniedHandler;
import com.example.auction.common.config.security.CustomAuthenticationEntryPoint;
import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.userchat.dto.response.UserChatMessageListResponse;
import com.example.auction.domain.userchat.dto.response.UserChatMessageResponse;
import com.example.auction.domain.userchat.dto.response.UserChatRoomResponse;
import com.example.auction.domain.userchat.service.UserChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserChatController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class UserChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserChatService userChatService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    @BeforeEach
    void setUpSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(USER_ID, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========================
    // 채팅방 목록 조회
    // ========================

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getRooms_success() throws Exception {
        // given
        List<UserChatRoomResponse> response = List.of(
                new UserChatRoomResponse(1L, 100L, 2L, LocalDateTime.now()),
                new UserChatRoomResponse(2L, 200L, 2L, LocalDateTime.now())
        );
        given(userChatService.getRooms(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/user-chat/rooms")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].auctionId").value(100L))
                .andExpect(jsonPath("$.data[0].counterpartId").value(2L))
                .andDo(document("user-chat/get-rooms",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공 - 채팅방 없음")
    void getRooms_success_empty() throws Exception {
        // given
        given(userChatService.getRooms(USER_ID)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/user-chat/rooms")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }


    // ========================
    // 메시지 목록 조회
    // ========================

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 없음")
    void getMessages_success_noCursor() throws Exception {
        // given
        List<UserChatMessageResponse> messages = List.of(
                new UserChatMessageResponse(1L, USER_ID, "안녕하세요", LocalDateTime.now()),
                new UserChatMessageResponse(2L, 2L,      "반갑습니다", LocalDateTime.now())
        );
        UserChatMessageListResponse response = new UserChatMessageListResponse(messages, null);
        given(userChatService.getMessages(eq(ROOM_ID), eq(USER_ID), eq(null), eq(20))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/user-chat/rooms/{roomId}/messages", ROOM_ID)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("메시지 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.messages[0].content").value("안녕하세요"))
                .andExpect(jsonPath("$.data.nextCursor").value((Object) null))
                .andDo(document("user-chat/get-messages",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("roomId").description("채팅방 식별자")),
                        queryParameters(
                                parameterWithName("cursor").description("이전 페이지 마지막 메시지 식별자").optional(),
                                parameterWithName("size").description("조회 개수 (기본값 20)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 있음 + nextCursor 반환")
    void getMessages_success_withCursor() throws Exception {
        // given
        List<UserChatMessageResponse> messages = List.of(
                new UserChatMessageResponse(5L, USER_ID, "이전 메시지", LocalDateTime.now())
        );
        UserChatMessageListResponse response = new UserChatMessageListResponse(messages, 5L);
        given(userChatService.getMessages(eq(ROOM_ID), eq(USER_ID), eq(30L), eq(20))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/user-chat/rooms/{roomId}/messages", ROOM_ID)
                        .header("Authorization", "Bearer accessToken")
                        .param("cursor", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.nextCursor").value(5L));
    }

    @Test
    @DisplayName("메시지 목록 조회 성공 - size 파라미터 지정")
    void getMessages_success_customSize() throws Exception {
        // given
        List<UserChatMessageResponse> messages = List.of(
                new UserChatMessageResponse(1L, USER_ID, "메시지", LocalDateTime.now())
        );
        UserChatMessageListResponse response = new UserChatMessageListResponse(messages, null);
        given(userChatService.getMessages(eq(ROOM_ID), eq(USER_ID), eq(null), eq(10))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/user-chat/rooms/{roomId}/messages", ROOM_ID)
                        .header("Authorization", "Bearer accessToken")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1));
    }
}