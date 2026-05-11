package com.example.auction.domain.category.dto.response;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CategoryListGetResponse {

    private final Long categoryId;
    private final String name;
    private final int depth;
    private final List<CategoryListGetResponse> children;

    public CategoryListGetResponse(Long categoryId, String name, int depth) {
        this.categoryId = categoryId;
        this.name = name;
        this.depth = depth;
        this.children = new ArrayList<>();
    }

    public void addChild(CategoryListGetResponse child) {
        this.children.add(child);
    }
}
