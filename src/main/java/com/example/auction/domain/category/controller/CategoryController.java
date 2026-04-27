package com.example.auction.domain.category.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    public ResponseEntity<BaseResponse<List<CategoryListGetResponse>>> getCategoryList() {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "카테고리 목록 조회 요청 성공", categoryService.getCategoryList()));
    }
}
