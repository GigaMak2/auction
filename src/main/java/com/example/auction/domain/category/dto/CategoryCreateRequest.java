package com.example.auction.domain.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequest(
        Long parentId,

        @NotBlank(message = "카테고리 이름을 입력해주세요")
        String name
) {}
