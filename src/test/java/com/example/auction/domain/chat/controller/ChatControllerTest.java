package com.example.auction.domain.chat.controller;

import com.example.auction.common.config.security.CustomAccessDeniedHandler;
import com.example.auction.common.config.security.CustomAuthenticationEntryPoint;
import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatMessageResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.dto.ChatRoomUpdateRequest;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.service.ChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ChatController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

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
    // 채팅방 생성
    // ========================

    @Test
    @DisplayName("채팅방 생성 성공")
    void createRoom_success() throws Exception {
        // given
        ChatRoomResponse response = new ChatRoomResponse(10L, null, LocalDateTime.now());
        given(chatService.createRoom(1L)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/chat/rooms")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방을 생성했습니다"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andDo(document("chat/create-room",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));
    }


    // ========================
    // 채팅방 목록 조회
    // ========================

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getRooms_success() throws Exception {
        // given
        List<ChatRoomResponse> response = List.of(
                new ChatRoomResponse(1L, "제목1", LocalDateTime.now()),
                new ChatRoomResponse(2L, "제목2", LocalDateTime.now())
        );
        given(chatService.getRooms(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/chat/rooms")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[1].id").value(2L))
                .andDo(document("chat/get-rooms",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공 - 채팅방 없음")
    void getRooms_success_empty() throws Exception {
        // given
        given(chatService.getRooms(1L)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }


    // ========================
    // 채팅방 제목 수정
    // ========================

    @Test
    @DisplayName("채팅방 제목 수정 성공")
    void updateRoom_success() throws Exception {
        // given
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest("새제목");
        ChatRoomResponse response = new ChatRoomResponse(10L, "새제목", LocalDateTime.now());
        given(chatService.updateTitle(eq(10L), eq(1L), eq("새제목"))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/{roomId}", 10L)
                        .header("Authorization", "Bearer accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 제목을 수정했습니다"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.title").value("새제목"))
                .andDo(document("chat/update-room",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("roomId").description("채팅방 식별자")),
                        requestFields(fieldWithPath("title").description("변경할 채팅방 제목 (10자 이하)"))
                ));
    }

    @Test
    @DisplayName("채팅방 제목 수정 실패 - 제목 공백")
    void updateRoom_fail_titleBlank() throws Exception {
        // given
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest("");

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("제목을 입력해 주세요"));
    }

    @Test
    @DisplayName("채팅방 제목 수정 실패 - 제목 10자 초과")
    void updateRoom_fail_titleTooLong() throws Exception {
        // given
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest("12345678901");

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("제목은 10자 이하로 입력해 주세요"));
    }


    // ========================
    // 채팅방 삭제
    // ========================

    @Test
    @DisplayName("채팅방 삭제 성공")
    void deleteRoom_success() throws Exception {
        // given
        doNothing().when(chatService).deleteRoom(10L, 1L);

        // when & then
        mockMvc.perform(delete("/api/chat/rooms/{roomId}", 10L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방을 삭제했습니다"))
                .andDo(document("chat/delete-room",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("roomId").description("채팅방 식별자"))
                ));

        verify(chatService).deleteRoom(10L, 1L);
    }


    // ========================
    // 메시지 목록 조회
    // ========================

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 없음")
    void getMessages_success_noCursor() throws Exception {
        // given
        List<ChatMessageResponse> messages = List.of(
                new ChatMessageResponse(1L, "안녕", MessageRole.USER, LocalDateTime.now()),
                new ChatMessageResponse(2L, "안녕하세요", MessageRole.ASSISTANT, LocalDateTime.now())
        );
        ChatMessageListResponse response = new ChatMessageListResponse(messages, null);
        given(chatService.getMessages(eq(10L), eq(1L), eq(null), eq(20))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", 10L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("메시지 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.nextCursor").value((Object) null))
                .andDo(document("chat/get-messages",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("roomId").description("채팅방 식별자")),
                        queryParameters(
                                parameterWithName("cursor").description("이전 페이지의 마지막 메시지 식별자").optional(),
                                parameterWithName("size").description("조회 개수 (기본값 20)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 있음 + nextCursor 반환")
    void getMessages_success_withCursor() throws Exception {
        // given
        List<ChatMessageResponse> messages = List.of(
                new ChatMessageResponse(5L, "이전 메시지", MessageRole.USER, LocalDateTime.now())
        );
        ChatMessageListResponse response = new ChatMessageListResponse(messages, 5L);
        given(chatService.getMessages(eq(10L), eq(1L), eq(30L), eq(20))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/10/messages")
                        .param("cursor", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.nextCursor").value(5L));
    }
}