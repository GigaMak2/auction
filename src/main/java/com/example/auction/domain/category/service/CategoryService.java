package com.example.auction.domain.category.service;

import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "getCategoryList", key = "'all'")
    public List<CategoryListGetResponse> getCategoryList() {
        List<Category> categoryList = categoryRepository.findAll();

        Map<Long, CategoryListGetResponse> map = new LinkedHashMap<>();
        for (Category category : categoryList) {
            map.put(category.getId(), new CategoryListGetResponse(
                    category.getId(),
                    category.getName(),
                    category.getDepth()
            ));
        }

        List<CategoryListGetResponse> roots = new ArrayList<>();
        for (Category category : categoryList) {
            CategoryListGetResponse node = map.get(category.getId());
            if (category.getParentId() == null) {
                roots.add(node);
            } else {
                map.get(category.getParentId()).addChild(node);
            }
        }

        return roots;
    }

    @Transactional(readOnly = true)
    public List<Long> collectDescendantIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();

        ids.add(categoryId);

        collectIds(categoryId, ids);
        return ids;
    }

    private void collectIds(Long categoryId, List<Long> ids) {
        List<Category> children = categoryRepository.findAllByParentId(categoryId);
        for (Category child : children) {
            ids.add(child.getId());
            collectIds(child.getId(), ids);
        }
    }
}
