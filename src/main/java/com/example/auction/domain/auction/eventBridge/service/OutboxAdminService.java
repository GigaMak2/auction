package com.example.auction.domain.auction.eventBridge.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.eventBridge.dto.OutboxAdminResponse;
import com.example.auction.domain.auction.eventBridge.entity.AuctionScheduleOutbox;
import com.example.auction.domain.auction.eventBridge.exception.OutboxErrorEnum;
import com.example.auction.domain.auction.eventBridge.repository.AuctionScheduleOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxAdminService {

    private final AuctionScheduleOutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public List<OutboxAdminResponse> getFailedOutbox() {
        return outboxRepository.findByStatus("FAILED").stream()
                .map(OutboxAdminResponse::from)
                .toList();
    }

    @Transactional
    public void retry(Long outboxId) {
        AuctionScheduleOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ServiceErrorException(OutboxErrorEnum.OUTBOX_NOT_FOUND));
        outbox.resetForRetry();
    }
}