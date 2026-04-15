package com.example.auction.domain.ai.service;

import com.example.auction.domain.ai.dto.ChatMessageCacheDto;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextCacheService {

    private static final String KEY_PREFIX = "chat:context:";
    private static final int MAX_MESSAGES = 20;      // AI 컨텍스트로 넘길 최대 메시지 수
    private static final Duration TTL = Duration.ofHours(24); // 마지막 갱신 후 24시간 유지

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;

    // AI 호출 전 컨텍스트 로드 — 캐시 히트 시 Redis, 미스 시 DB 조회 후 캐싱
    public List<ChatMessageCacheDto> getContext(Long roomId) {
        String key = KEY_PREFIX + roomId;
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            return deserialize(json);
        }

        // 캐시 미스 — DB에서 최근 N개 조회 후 캐시 적재 (첫 메시지 or Redis 만료 후)
        List<ChatMessageCacheDto> messages = chatMessageRepository.findRecentByRoomId(roomId, MAX_MESSAGES)
                .stream()
                .map(m -> new ChatMessageCacheDto(m.getRole().name(), m.getContent()))
                .toList();

        if (!messages.isEmpty()) {
            save(key, messages);
        }

        return messages;
    }

    // AI 응답 완료 후 유저 메시지 + AI 응답을 캐시에 추가 — doFinally에서 호출
    public void appendMessages(Long roomId, String userContent, String assistantContent) {
        String key = KEY_PREFIX + roomId;
        String json = stringRedisTemplate.opsForValue().get(key);

        List<ChatMessageCacheDto> messages = new ArrayList<>(json != null ? deserialize(json) : List.of());
        messages.add(new ChatMessageCacheDto("USER", userContent));
        messages.add(new ChatMessageCacheDto("ASSISTANT", assistantContent));

        // MAX_MESSAGES 초과 시 오래된 것부터 제거 (슬라이딩 윈도우)
        if (messages.size() > MAX_MESSAGES) {
            messages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
        }

        save(key, messages);
    }

    // 채팅방 삭제 시 컨텍스트 캐시 제거
    public void evict(Long roomId) {
        stringRedisTemplate.delete(KEY_PREFIX + roomId);
    }

    // JSON 직렬화 후 Redis에 저장 + TTL 갱신
    private void save(String key, List<ChatMessageCacheDto> messages) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(messages), TTL);
        } catch (JsonProcessingException e) {
            log.warn("[ChatContextCacheService] 캐시 저장 실패 key={}", key, e);
        }
    }

    // JSON 역직렬화 — 파싱 실패 시 빈 리스트 반환 (캐시 오류가 AI 응답을 막지 않도록)
    private List<ChatMessageCacheDto> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("[ChatContextCacheService] 캐시 역직렬화 실패, 빈 컨텍스트로 대체", e);
            return List.of();
        }
    }
}