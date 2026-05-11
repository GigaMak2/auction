package com.example.auction.domain.category.dto.response;

import java.time.LocalDateTime;

public record CategoryRenameResponse(
        Long categoryId,
        String name,
        LocalDateTime modifiedAt
) {}
