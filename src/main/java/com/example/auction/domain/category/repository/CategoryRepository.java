package com.example.auction.domain.category.repository;

import com.example.auction.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByParentIdIsNullAndName(String name);

    boolean existsByParentIdAndName(Long parentId, String name);
}
