package com.example.auction.domain.category.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/api/admin/categories")
    public ResponseEntity<BaseResponse<CategoryCreateResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "카테고리 생성 요청 성공", categoryService.createCategory(request)));
    }

    @PatchMapping("/api/admin/categories/{categoryId}/name")
    public ResponseEntity<BaseResponse<CategoryRenameResponse>> renameCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRenameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리 이름 수정 요청 성공", categoryService.renameCategory(categoryId, request)));
    }

    @PatchMapping("/api/admin/categories/{categoryId}/parent")
    public ResponseEntity<BaseResponse<CategoryMoveResponse>> moveCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryMoveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리 이동 요청 성공", categoryService.moveCategory(categoryId, request)));
    }

    @GetMapping("/api/categories")
    public ResponseEntity<BaseResponse<List<CategoryListGetResponse>>> getCategoryList() {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리 목록 조회 요청 성공", categoryService.getCategoryList()));
    }
}
