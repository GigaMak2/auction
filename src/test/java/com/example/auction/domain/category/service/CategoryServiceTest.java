package com.example.auction.domain.category.service;

import com.example.auction.domain.category.dto.response.CategoryListGetResponse;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;


    // ========================
    // 카테고리 목록 조회
    // ========================

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 트리 구조 반환")
    void getCategoryList_success() {
        // given
        Category root = Category.root("전자기기");
        ReflectionTestUtils.setField(root, "id", 1L);

        Category child = Category.child(1L, "스마트폰", 0);
        ReflectionTestUtils.setField(child, "id", 2L);

        Category grandChild = Category.child(2L, "아이폰", 1);
        ReflectionTestUtils.setField(grandChild, "id", 3L);

        given(categoryRepository.findAll()).willReturn(List.of(root, child, grandChild));

        // when
        List<CategoryListGetResponse> categoryList = categoryService.getCategoryList();

        // then
        assertThat(categoryList).hasSize(1);

        CategoryListGetResponse rootResponse = categoryList.get(0);
        assertThat(rootResponse.getCategoryId()).isEqualTo(1L);
        assertThat(rootResponse.getName()).isEqualTo("전자기기");
        assertThat(rootResponse.getChildren()).hasSize(1);

        CategoryListGetResponse childResponse = rootResponse.getChildren().get(0);
        assertThat(childResponse.getCategoryId()).isEqualTo(2L);
        assertThat(childResponse.getName()).isEqualTo("스마트폰");
        assertThat(childResponse.getChildren()).hasSize(1);

        CategoryListGetResponse grandChildResponse = childResponse.getChildren().get(0);
        assertThat(grandChildResponse.getCategoryId()).isEqualTo(3L);
        assertThat(grandChildResponse.getName()).isEqualTo("아이폰");
        assertThat(grandChildResponse.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 카테고리 없음")
    void getCategoryList_success_empty() {
        // given
        given(categoryRepository.findAll()).willReturn(List.of());

        // when
        List<CategoryListGetResponse> categoryList = categoryService.getCategoryList();

        // then
        assertThat(categoryList).isEmpty();
    }


    // ========================
    // 하위 카테고리 ID 수집
    // ========================

    @Test
    @DisplayName("하위 카테고리 ID 수집 성공 - 자식 없음")
    void collectDescendantIds_success_noChildren() {
        // given
        Long categoryId = 1L;

        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of());

        // when
        List<Long> ids = categoryService.collectDescendantIds(categoryId);

        // then
        assertThat(ids).containsExactly(categoryId);
    }

    @Test
    @DisplayName("하위 카테고리 ID 수집 성공 - 자식 있음")
    void collectDescendantIds_success_hasChildren() {
        // given
        Long categoryId = 1L;
        Long childId = 2L;
        Long grandChildId = 3L;

        Category child = Category.child(categoryId, "스마트폰", 0);
        ReflectionTestUtils.setField(child, "id", childId);

        Category grandChild = Category.child(childId, "아이폰", 1);
        ReflectionTestUtils.setField(grandChild, "id", grandChildId);

        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of(child));
        given(categoryRepository.findAllByParentId(childId)).willReturn(List.of(grandChild));
        given(categoryRepository.findAllByParentId(grandChildId)).willReturn(List.of());

        // when
        List<Long> ids = categoryService.collectDescendantIds(categoryId);

        // then
        assertThat(ids).containsExactly(categoryId, childId, grandChildId);
    }
}