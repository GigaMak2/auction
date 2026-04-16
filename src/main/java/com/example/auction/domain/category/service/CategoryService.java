package com.example.auction.domain.category.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.category.dto.CategoryCreateRequest;
import com.example.auction.domain.category.dto.CategoryCreateResponse;
import com.example.auction.domain.category.dto.CategoryRenameRequest;
import com.example.auction.domain.category.dto.CategoryRenameResponse;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.exception.CategoryErrorEnum;
import com.example.auction.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private static final int MAX_DEPTH = 2;

    @Transactional
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
    public CategoryRenameResponse renameCategory(Long categoryId, CategoryRenameRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

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
}
