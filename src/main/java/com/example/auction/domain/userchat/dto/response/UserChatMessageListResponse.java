package com.example.auction.domain.userchat.dto.response;

import java.util.List;

public record UserChatMessageListResponse(
        List<UserChatMessageResponse> messages,
        Long nextCursor
) {
    public static UserChatMessageListResponse of(List<UserChatMessageResponse> messages, int size) {
        boolean hasNext = messages.size() == size;
        Long nextCursor = hasNext ? messages.getFirst().id() : null;
        return new UserChatMessageListResponse(messages, nextCursor);
    }
}