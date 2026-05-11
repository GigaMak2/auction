package com.example.auction.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRenameRequest(
        @NotBlank(message = "카테고리 이름을 입력해주세요")
        String name
) {}
