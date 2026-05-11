package com.example.auction.domain.category.dto.response;

import java.time.LocalDateTime;

public record CategoryMoveResponse(
        Long categoryId,
        Long parentId,
        String name,
        LocalDateTime modifiedAt
) {}
