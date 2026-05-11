package com.example.auction.domain.category.dto.response;

import java.time.LocalDateTime;

public record CategoryCreateResponse(
        Long categoryId,
        Long parentId,
        String name,
        LocalDateTime createdAt
) {}
