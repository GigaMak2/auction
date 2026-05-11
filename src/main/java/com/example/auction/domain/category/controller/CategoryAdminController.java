package com.example.auction.domain.category.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    @PostMapping
    public ResponseEntity<BaseResponse<CategoryCreateResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "카테고리를 생성했습니다", categoryAdminService.createCategory(request)));
    }

    @PatchMapping("/{categoryId}/name")
    public ResponseEntity<BaseResponse<CategoryRenameResponse>> renameCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRenameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리 이름을 수정했습니다", categoryAdminService.renameCategory(categoryId, request)));
    }

    @PatchMapping("/{categoryId}/parent")
    public ResponseEntity<BaseResponse<CategoryMoveResponse>> moveCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryMoveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리를 이동했습니다", categoryAdminService.moveCategory(categoryId, request)));
    }
}
