package com.example.auction.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CategoryCreateRequest(
        @Positive(message = "카테고리 아이디는 1 이상이어야 합니다")
        Long parentId,

        @NotBlank(message = "카테고리 이름을 입력해주세요")
        String name
) {}
