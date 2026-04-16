package com.example.auction.domain.category.dto;

import java.time.LocalDateTime;

public record CategoryMoveResponse(
        Long categoryId,
        Long parentId,
        String name,
        LocalDateTime modifiedAt
) {}
