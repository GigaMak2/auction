package com.example.auction.domain.category.dto;

import java.time.LocalDateTime;

public record CategoryRenameResponse(
        Long categoryId,
        String name,
        LocalDateTime modifiedAt
) {}
