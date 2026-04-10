package com.example.auction.domain.chat.dto;

import java.util.List;

public record ChatMessageListResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor  // 다음 페이지 커서 (null이면 마지막 페이지)
) {
    /**
     * Create a paginated chat message list response.
     *
     * <p>If the provided list length equals the requested page size, the response's `nextCursor`
     * is set to the oldest message's `id` (messages.get(0).id()) to request older messages using
     * an `id < cursor` query; otherwise `nextCursor` is `null` indicating no further pages.</p>
     *
     * @param messages the messages included in the current page (expected ordered newest last / oldest first)
     * @param size the requested page size used to determine whether a next page exists
     * @return a ChatMessageListResponse containing the messages and a `nextCursor` when more pages exist, or `null` for the cursor when this is the last page
     */
    public static ChatMessageListResponse of(List<ChatMessageResponse> messages, int size) {
        // 요청한 size만큼 왔으면 다음 페이지 존재 — 가장 오래된 메시지 id를 커서로 (id < cursor 조건으로 더 오래된 메시지 조회)
        boolean hasNext = messages.size() == size;
        Long nextCursor = hasNext ? messages.get(0).id() : null;
        return new ChatMessageListResponse(messages, nextCursor);
    }
}