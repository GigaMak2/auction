package com.example.auction.domain.category.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.exception.CategoryErrorEnum;
import com.example.auction.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;

    private static final int MAX_DEPTH = 2;

    @Transactional
    @CacheEvict(cacheNames = "getCategoryList", key = "'all'")
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {
        Category category = request.parentId() == null ? rootCategory(request.name()) : childCategory(request.parentId(), request.name());
        categoryRepository.save(category);

        return new CategoryCreateResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getCreatedAt()
        );
    }

    @Transactional
    @CacheEvict(cacheNames = "getCategoryList", key = "'all'")
    public CategoryRenameResponse renameCategory(Long categoryId, CategoryRenameRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

        if (category.getName().equals(request.name())) {
            throw new ServiceErrorException(CategoryErrorEnum.SAME_NAME_CATEGORY);
        }

        if (category.getParentId() == null) {
            if (categoryRepository.existsByParentIdIsNullAndName(request.name())) {
                throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
            }
        } else {
            if (categoryRepository.existsByParentIdAndName(category.getParentId(), request.name())) {
                throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
            }
        }

        category.rename(request.name());

        return new CategoryRenameResponse(category.getId(), category.getName(), category.getModifiedAt());
    }

    @Transactional
    @CacheEvict(cacheNames = "getCategoryList", key = "'all'")
    public CategoryMoveResponse moveCategory(Long categoryId, CategoryMoveRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

        if (Objects.equals(category.getParentId(), request.parentId())) {
            throw new ServiceErrorException(CategoryErrorEnum.SAME_LOCATION_CATEGORY);
        }

        if (request.parentId() != null && category.getId().equals(request.parentId())) {
            throw new ServiceErrorException(CategoryErrorEnum.CATEGORY_CANNOT_BE_OWN_PARENT);
        }

        int newDepth;
        if (request.parentId() == null) {
            if (categoryRepository.existsByParentIdIsNullAndName(category.getName())) {
                throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
            }
            newDepth = 0;
        } else {
            Category newParent = categoryRepository.findById(request.parentId()).orElseThrow(
                    () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

            // 순환 참조 - 새 부모의 조상을 타고 올라가다가 자기 자신이 나오면 예외 던짐
            checkCircularReference(category.getId(), newParent);

            newDepth = newParent.getDepth() + 1;

            if (newDepth + getSubtreeHeight(category.getId()) > MAX_DEPTH) {
                throw new ServiceErrorException(CategoryErrorEnum.CATEGORY_MAX_DEPTH_EXCEEDED);
            }

            if (categoryRepository.existsByParentIdAndName(request.parentId(), category.getName())) {
                throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
            }
        }

        updateChildrenDepth(category.getId(), newDepth - category.getDepth());
        category.move(request.parentId(), newDepth);

        return new CategoryMoveResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getModifiedAt());
    }

    private Category rootCategory(String name) {
        if (categoryRepository.existsByParentIdIsNullAndName(name)) {
            throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
        }

        return Category.root(name);
    }

    private Category childCategory(Long parentId, String name) {
        Category parent = categoryRepository.findById(parentId).orElseThrow(
                () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

        if (parent.getDepth() >= MAX_DEPTH) {
            throw new ServiceErrorException(CategoryErrorEnum.CATEGORY_MAX_DEPTH_EXCEEDED);
        }

        if (categoryRepository.existsByParentIdAndName(parent.getId(), name)) {
            throw new ServiceErrorException(CategoryErrorEnum.DUPLICATED_CATEGORY);
        }

        return  Category.child(parent.getId(), name, parent.getDepth());
    }

    private void checkCircularReference(Long categoryId, Category target) {
        Long currentParentId = target.getParentId();

        while (currentParentId != null) {
            if (currentParentId.equals(categoryId)) {
                throw new ServiceErrorException(CategoryErrorEnum.CATEGORY_CIRCULAR_REFERENCE);
            }

            Category ancestor = categoryRepository.findById(currentParentId).orElseThrow(
                    () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));
            currentParentId = ancestor.getParentId();
        }
    }

    private int getSubtreeHeight(Long categoryId) {
        List<Category> children = categoryRepository.findAllByParentId(categoryId);
        if (children.isEmpty()) return 0;

        int maxHeight = 0;
        for (Category child : children) {
            int childHeight = getSubtreeHeight(child.getId());
            if (childHeight > maxHeight) {
                maxHeight = childHeight;
            }
        }
        return 1 + maxHeight;
    }

    private void updateChildrenDepth(Long categoryId, int depthDiff) {
        if (depthDiff == 0) return;

        List<Category> children = categoryRepository.findAllByParentId(categoryId);
        for (Category child : children) {
            child.updateDepth(child.getDepth() + depthDiff);
            updateChildrenDepth(child.getId(), depthDiff);
        }
    }
}
