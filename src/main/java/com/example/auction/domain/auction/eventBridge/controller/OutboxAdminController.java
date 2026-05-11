package com.example.auction.domain.auction.eventBridge.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.auction.eventBridge.dto.OutboxAdminResponse;
import com.example.auction.domain.auction.eventBridge.service.OutboxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/outbox")
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    // 실패한 outbox 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<List<OutboxAdminResponse>>> getFailedOutbox() {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.name(), "FAILED outbox 조회에 성공했습니다", outboxAdminService.getFailedOutbox()));
    }

    // 재시도
    @PostMapping("/{outboxId}/retry")
    public ResponseEntity<BaseResponse<Void>> retry(@PathVariable Long outboxId) {
        outboxAdminService.retry(outboxId);
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.name(), "재시도 요청을 성공했습니다", null));
    }
}
