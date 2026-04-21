package com.example.auction.domain.category.repository;

import com.example.auction.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByParentIdIsNullAndName(String name);

    boolean existsByParentIdAndName(Long parentId, String name);

    List<Category> findAllByParentId(Long parentId);

    List<Category> findByNameContainingIgnoreCase(String name);

    List<Category> findAllByParentIdIn(Collection<Long> parentIds);
}
