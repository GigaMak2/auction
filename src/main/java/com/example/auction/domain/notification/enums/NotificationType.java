package com.example.auction.domain.notification.enums;

import com.example.auction.domain.notification.dto.NotificationMessage;

public enum NotificationType {
    AUCTION_STARTED {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "'" + message.itemName() + "' 경매가 시작됐습니다.";
        }
    },
    NEW_BID {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "'" + message.itemName() + "' 경매에 새 입찰이 들어왔습니다.";
        }
    },
    LOWEST_BID_UPDATED {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "'" + message.itemName() + "' 경매에 더 낮은 입찰이 들어왔습니다. 재입찰을 고려해보세요.";
        }
    },
    AUCTION_CLOSED_WIN {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "축하합니다! '" + message.itemName() + "' 경매에 낙찰됐습니다.";
        }
    },
    AUCTION_CLOSED_BUYER {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "'" + message.itemName() + "' 경매의 낙찰자가 결정됐습니다.";
        }
    },
    AUCTION_NO_BID {
        @Override
        public String generateMessage(NotificationMessage message) {
            return "'" + message.itemName() + "' 경매가 입찰자 없이 종료됐습니다.";
        }
    };

    public abstract String generateMessage(NotificationMessage message);
}