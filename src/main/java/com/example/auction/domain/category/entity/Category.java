package com.example.auction.domain.category.entity;

import com.example.auction.common.entity.ModifiableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "categories", uniqueConstraints ={
        @UniqueConstraint(columnNames = {"parent_id", "name"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int depth;

    public static Category root(String name) {
        Category category = new Category();
        category.parentId = null;
        category.name = name;
        category.depth = 0;

        return category;
    }

    public static Category child(Long parentId, String name, int parentDepth) {
        Category category = new Category();
        category.parentId = parentId;
        category.name = name;
        category.depth = parentDepth + 1;

        return category;
    }
}
