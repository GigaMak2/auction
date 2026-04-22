package com.example.auction.domain.notification.dto;

import com.example.auction.domain.notification.enums.NotificationType;

public record NotificationMessage(
        NotificationType type,
        Long receiverId,
        Long auctionId,
        String itemName
) {}
