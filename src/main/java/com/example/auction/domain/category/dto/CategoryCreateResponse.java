package com.example.auction.domain.category.dto;

import java.time.LocalDateTime;

public record CategoryCreateResponse(
        Long categoryId,
        Long parentId,
        String name,
        LocalDateTime createdAt
) {}
