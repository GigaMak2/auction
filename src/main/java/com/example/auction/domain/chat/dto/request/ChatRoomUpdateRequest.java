package com.example.auction.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRoomUpdateRequest(
        @NotBlank(message = "제목을 입력해 주세요")
        @Size(max = 10, message = "제목은 10자 이하로 입력해 주세요")
        String title
) {}