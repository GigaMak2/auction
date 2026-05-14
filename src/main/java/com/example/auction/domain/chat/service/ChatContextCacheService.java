package com.example.auction.domain.chat.service;

import com.example.auction.domain.chat.dto.ChatMessageCacheDto;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
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
    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;

    // AI 호출 전 컨텍스트 로드 — 캐시 히트 시 Redis, 미스 시 DB 조회 후 캐싱
    // Redis 장애 시 예외를 삼키고 DB 폴백 — Redis 문제가 채팅을 막지 않도록
    public List<ChatMessageCacheDto> getContext(Long roomId) {
        String key = KEY_PREFIX + roomId;

        try {
            List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (jsonList != null && !jsonList.isEmpty()) {
                List<ChatMessageCacheDto> cached = deserialize(key, jsonList);
                if (cached != null) {
                    return cached;
                }
                // deserialize 실패(손상) — DB 재조회로 폴백
            }
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] Redis 조회 실패, DB 폴백 key={}", key, e); // Redis 장애 감지 — 폴백 빈도로 Redis 상태 모니터링 가능
        }

        // 캐시 미스 or Redis 장애 — DB에서 최근 N개 조회 후 캐시 적재 (첫 메시지 or Redis 만료 후)
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
    // GET 없이 RPUSH로 직접 추가 — 동시 요청 시 덮어쓰기 없이 순서대로 누적
    // Redis 장애 시 무시 — 다음 턴 getContext()의 DB 폴백으로 복구됨
    public void appendMessages(Long roomId, String userContent, String assistantContent) {
        String key = KEY_PREFIX + roomId;

        try {
            String userJson = objectMapper.writeValueAsString(new ChatMessageCacheDto("USER", userContent));
            String assistantJson = objectMapper.writeValueAsString(new ChatMessageCacheDto("ASSISTANT", assistantContent));

            stringRedisTemplate.opsForList().rightPushAll(key, userJson, assistantJson);
            stringRedisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1); // 슬라이딩 윈도우
            stringRedisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 추가 실패, 다음 턴 DB 폴백으로 복구 key={}", key, e); // Redis 장애 감지 — AI 응답 저장 실패 빈도 모니터링 가능
        }
    }

    // 채팅방 삭제 시 컨텍스트 캐시 제거
    // Redis 장애 시 무시 — 채팅방 삭제 자체는 성공해야 함
    public void evict(Long roomId) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + roomId);
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 evict 실패 roomId={}", roomId, e); // Redis 장애 감지 — 채팅방 삭제 시 캐시 미제거는 TTL 만료 시 자동 해소
        }
    }

    // DB 폴백 후 캐시 백필 — LLEN 체크 후 빈 키에만 RPUSH (best-effort, 모든 예외 흡수)
    // delete + rightPushAll 구조는 동시 appendMessages()가 끼어들면 새 메시지를 덮어쓰는 경합 발생
    // size() == 0 일 때만 백필하므로 다른 스레드가 이미 쓴 경우 건너뜀
    private void save(String key, List<ChatMessageCacheDto> messages) {
        try {
            List<String> jsonList = new ArrayList<>();
            for (ChatMessageCacheDto m : messages) {
                jsonList.add(objectMapper.writeValueAsString(m));
            }
            Long len = stringRedisTemplate.opsForList().size(key);
            if (len == null || len == 0) {
                stringRedisTemplate.opsForList().rightPushAll(key, jsonList);
                stringRedisTemplate.expire(key, TTL);
            }
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 저장 실패 key={}", key, e); // Redis 장애 감지 — DB 폴백 후 캐시 백필 실패, 다음 요청에서 재시도됨
        }
    }

    // 개별 JSON 역직렬화 — 파싱 실패 시 null 반환 (호출부에서 손상된 키 삭제 후 DB 재조회)
    private List<ChatMessageCacheDto> deserialize(String key, List<String> jsonList) {
        try {
            List<ChatMessageCacheDto> result = new ArrayList<>();
            for (String json : jsonList) {
                result.add(objectMapper.readValue(json, ChatMessageCacheDto.class));
            }
            return result;
        } catch (JacksonException e) {
            log.warn("[ChatContextCacheService] 캐시 역직렬화 실패, 손상된 키 제거 후 DB 재조회 key={}", key, e); // Redis 데이터 손상 감지 — 반복 발생 시 직렬화 스키마 변경 의심
            stringRedisTemplate.delete(key);
            return null;
        }
    }
}