package com.example.auction.domain.chat.service;

import com.example.auction.domain.chat.dto.ChatMessageCacheDto;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatContextCacheServiceTest {

    @InjectMocks
    private ChatContextCacheService chatContextCacheService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ListOperations<String, String> listOperations;

    private static final Long ROOM_ID = 1L;
    private static final String KEY = "chat:context:1";


    // ========================
    // getContext
    // ========================

    @Test
    @DisplayName("getContext - 캐시 히트: Redis에 데이터 있으면 역직렬화해서 반환")
    void getContext_cacheHit() throws Exception {
        // given
        List<String> jsonList = List.of(
                objectMapper.writeValueAsString(new ChatMessageCacheDto("USER", "안녕")),
                objectMapper.writeValueAsString(new ChatMessageCacheDto("ASSISTANT", "안녕하세요"))
        );

        given(stringRedisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(KEY, 0, -1)).willReturn(jsonList);

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("USER");
        assertThat(result.get(0).content()).isEqualTo("안녕");
        assertThat(result.get(1).role()).isEqualTo("ASSISTANT");
        assertThat(result.get(1).content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("getContext - 캐시 미스, DB에 데이터 있음: DB 조회 후 Redis List에 저장 후 반환")
    void getContext_cacheMiss_dbHasData() throws Exception {
        // given
        ChatMessage msg1 = ChatMessage.of(ROOM_ID, "안녕", MessageRole.USER);
        ChatMessage msg2 = ChatMessage.of(ROOM_ID, "안녕하세요", MessageRole.ASSISTANT);

        given(stringRedisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(KEY, 0, -1)).willReturn(List.of());
        given(chatMessageRepository.findRecentByRoomId(ROOM_ID, 20)).willReturn(List.of(msg1, msg2));
        given(listOperations.size(KEY)).willReturn(0L);

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("USER");
        assertThat(result.get(1).role()).isEqualTo("ASSISTANT");

        verify(listOperations).size(KEY);
        verify(listOperations).rightPushAll(eq(KEY), anyList());
        verify(stringRedisTemplate).expire(KEY, Duration.ofHours(24));
    }

    @Test
    @DisplayName("getContext - 캐시 미스, DB도 비어있음: 빈 리스트 반환 + Redis 저장 안 함")
    void getContext_cacheMiss_dbEmpty() {
        // given
        given(stringRedisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(KEY, 0, -1)).willReturn(List.of());
        given(chatMessageRepository.findRecentByRoomId(ROOM_ID, 20)).willReturn(List.of());

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).isEmpty();
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
    }


    // ========================
    // appendMessages
    // ========================

    @Test
    @DisplayName("appendMessages - 유저·AI 메시지를 RPUSH로 추가하고 LTRIM으로 슬라이딩 윈도우 적용")
    void appendMessages_pushesAndTrims() throws Exception {
        // given
        given(stringRedisTemplate.opsForList()).willReturn(listOperations);

        // when
        chatContextCacheService.appendMessages(ROOM_ID, "질문", "답변");

        // then — rightPushAll에 전달된 JSON 검증
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assistantCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPushAll(eq(KEY), userCaptor.capture(), assistantCaptor.capture());

        ChatMessageCacheDto userMsg = objectMapper.readValue(userCaptor.getValue(), ChatMessageCacheDto.class);
        ChatMessageCacheDto assistantMsg = objectMapper.readValue(assistantCaptor.getValue(), ChatMessageCacheDto.class);
        assertThat(userMsg.role()).isEqualTo("USER");
        assertThat(userMsg.content()).isEqualTo("질문");
        assertThat(assistantMsg.role()).isEqualTo("ASSISTANT");
        assertThat(assistantMsg.content()).isEqualTo("답변");

        // 슬라이딩 윈도우 + TTL 갱신 확인
        verify(listOperations).trim(KEY, -20, -1);
        verify(stringRedisTemplate).expire(KEY, Duration.ofHours(24));
    }


    // ========================
    // evict
    // ========================

    @Test
    @DisplayName("evict - 채팅방 삭제 시 Redis 키 제거")
    void evict_deletesRedisKey() {
        // when
        chatContextCacheService.evict(ROOM_ID);

        // then
        verify(stringRedisTemplate).delete(KEY);
    }
}
