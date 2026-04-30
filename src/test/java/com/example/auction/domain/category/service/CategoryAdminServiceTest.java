package com.example.auction.domain.category.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.exception.CategoryErrorEnum;
import com.example.auction.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CategoryAdminServiceTest {

    @InjectMocks
    private CategoryAdminService categoryAdminService;

    @Mock
    private CategoryRepository categoryRepository;


    // ========================
    // 카테고리 생성
    // ========================

    @Test
    @DisplayName("카테고리 생성 성공 - root")
    void createCategory_success_root() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "전자기기");

        given(categoryRepository.existsByParentIdIsNullAndName("전자기기")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category category = inv.getArgument(0);
            ReflectionTestUtils.setField(category, "id", 1L);
            return category;
        });

        // when
        CategoryCreateResponse response = categoryAdminService.createCategory(request);

        // then
        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.parentId()).isNull();
        assertThat(response.name()).isEqualTo("전자기기");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 생성 성공 - child")
    void createCategory_success_child() {
        // given
        Long parentId = 1L;

        Category parent = Category.root("전자기기");
        ReflectionTestUtils.setField(parent, "id", parentId);

        CategoryCreateRequest request = new CategoryCreateRequest(parentId, "스마트폰");

        given(categoryRepository.findById(parentId)).willReturn(Optional.of(parent));
        given(categoryRepository.existsByParentIdAndName(parentId, "스마트폰")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category category = inv.getArgument(0);
            ReflectionTestUtils.setField(category, "id", 2L);
            return category;
        });

        // when
        CategoryCreateResponse response = categoryAdminService.createCategory(request);

        // then
        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.parentId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("스마트폰");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - root 중복")
    void createCategory_fail_rootDuplicated() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "전자기기");

        given(categoryRepository.existsByParentIdIsNullAndName("전자기기")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.createCategory(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 부모 카테고리 없음")
    void createCategory_fail_parentNotFound() {
        // given
        Long parentId = 99L;
        CategoryCreateRequest request = new CategoryCreateRequest(parentId, "스마트폰");

        given(categoryRepository.findById(parentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryAdminService.createCategory(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 최대 깊이 초과")
    void createCategory_fail_maxDepthExceeded() {
        // given
        Long parentId = 3L;

        // depth=2인 카테고리
        Category parent = Category.root("아이폰");
        ReflectionTestUtils.setField(parent, "id", parentId);
        ReflectionTestUtils.setField(parent, "depth", 2);

        CategoryCreateRequest request = new CategoryCreateRequest(parentId, "아이폰 17");

        given(categoryRepository.findById(parentId)).willReturn(Optional.of(parent));

        // when & then
        assertThatThrownBy(() -> categoryAdminService.createCategory(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_MAX_DEPTH_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("카테고리 생성 실패 - child 중복")
    void createCategory_fail_childDuplicated() {
        // given
        Long parentId = 1L;

        Category parent = Category.root("전자기기");
        ReflectionTestUtils.setField(parent, "id", parentId);

        CategoryCreateRequest request = new CategoryCreateRequest(parentId, "스마트폰");

        given(categoryRepository.findById(parentId)).willReturn(Optional.of(parent));
        given(categoryRepository.existsByParentIdAndName(parentId, "스마트폰")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.createCategory(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }


    // ========================
    // 카테고리 이름 수정
    // ========================

    @Test
    @DisplayName("카테고리 이름 수정 성공 - root")
    void renameCategory_success_root() {
        // given
        Long categoryId = 1L;

        Category category = Category.root("전자기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryRenameRequest request = new CategoryRenameRequest("가전제품");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdIsNullAndName("가전제품")).willReturn(false);

        // when
        CategoryRenameResponse response = categoryAdminService.renameCategory(categoryId, request);

        // then
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("가전제품");
    }

    @Test
    @DisplayName("카테고리 이름 수정 성공 - child")
    void renameCategory_success_child() {
        // given
        Long parentId = 1L;
        Long categoryId = 2L;

        Category category = Category.child(parentId, "스마트폰", 0);
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryRenameRequest request = new CategoryRenameRequest("태블릿");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdAndName(parentId, "태블릿")).willReturn(false);

        // when
        CategoryRenameResponse response = categoryAdminService.renameCategory(categoryId, request);

        // then
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("태블릿");
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - 카테고리 없음")
    void renameCategory_fail_categoryNotFound() {
        // given
        Long categoryId = 99L;
        CategoryRenameRequest request = new CategoryRenameRequest("가전제품");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryAdminService.renameCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - 동일한 이름")
    void renameCategory_fail_sameName() {
        // given
        Long categoryId = 1L;
        Category category = Category.root("전자제품");
        ReflectionTestUtils.setField(category, "id", categoryId);
        CategoryRenameRequest request = new CategoryRenameRequest("전자제품");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // when & then
        assertThatThrownBy(() -> categoryAdminService.renameCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.SAME_NAME_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - root 중복")
    void renameCategory_fail_rootDuplicated() {
        // given
        Long categoryId = 1L;

        Category category = Category.root("전자기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryRenameRequest request = new CategoryRenameRequest("가전제품");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdIsNullAndName("가전제품")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.renameCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - child 중복")
    void renameCategory_fail_childDuplicated() {
        // given
        Long parentId = 1L;
        Long categoryId = 2L;

        Category category = Category.child(parentId, "스마트폰", 0);
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryRenameRequest request = new CategoryRenameRequest("태블릿");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdAndName(parentId, "태블릿")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.renameCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }


    // ========================
    // 카테고리 이동
    // ========================

    @Test
    @DisplayName("카테고리 이동 성공 - root로 이동")
    void moveCategory_success_toRoot() {
        // given
        Long categoryId = 2L;

        Category category = Category.child(1L, "스마트폰", 0);
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryMoveRequest request = new CategoryMoveRequest(null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdIsNullAndName("스마트폰")).willReturn(false);
        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of());

        // when
        CategoryMoveResponse response = categoryAdminService.moveCategory(categoryId, request);

        // then
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.parentId()).isNull();
        assertThat(response.name()).isEqualTo("스마트폰");
    }

    @Test
    @DisplayName("카테고리 이동 성공 - 다른 부모로 이동")
    void moveCategory_success_toNewParent() {
        // given
        Long categoryId = 2L;
        Long newParentId = 1L;

        Category category = Category.root("스마트폰");
        ReflectionTestUtils.setField(category, "id", categoryId);

        Category newParent = Category.root("전자기기");
        ReflectionTestUtils.setField(newParent, "id", newParentId);

        CategoryMoveRequest request = new CategoryMoveRequest(newParentId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.of(newParent));
        given(categoryRepository.existsByParentIdAndName(newParentId, "스마트폰")).willReturn(false);
        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of());

        // when
        CategoryMoveResponse response = categoryAdminService.moveCategory(categoryId, request);

        // then
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.parentId()).isEqualTo(newParentId);
        assertThat(response.name()).isEqualTo("스마트폰");
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 카테고리 없음")
    void moveCategory_fail_categoryNotFound() {
        // given
        Long categoryId = 99L;
        CategoryMoveRequest request = new CategoryMoveRequest(null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 동일한 위치")
    void moveCategory_fail_sameLocation() {
        // given
        Long categoryId = 1L;
        Category category = Category.root("전자제품");
        ReflectionTestUtils.setField(category, "id", categoryId);
        CategoryMoveRequest request = new CategoryMoveRequest(null); // root → root

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.SAME_LOCATION_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 자기 자신을 부모로 지정")
    void moveCategory_fail_ownParent() {
        // given
        Long categoryId = 1L;

        Category category = Category.root("전자기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryMoveRequest request = new CategoryMoveRequest(categoryId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_CANNOT_BE_OWN_PARENT.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - root로 이동 시 중복")
    void moveCategory_fail_rootDuplicated() {
        // given
        Long categoryId = 2L;

        Category category = Category.child(1L, "스마트폰", 0);
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryMoveRequest request = new CategoryMoveRequest(null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentIdIsNullAndName("스마트폰")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 새 부모 없음")
    void moveCategory_fail_newParentNotFound() {
        // given
        Long categoryId = 1L;
        Long newParentId = 99L;

        Category category = Category.root("스마트폰");
        ReflectionTestUtils.setField(category, "id", categoryId);

        CategoryMoveRequest request = new CategoryMoveRequest(newParentId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 순환 참조")
    void moveCategory_fail_circularReference() {
        // given
        Long categoryId = 1L;
        Long newParentId = 2L;

        // 원래 스마트폰이 전자기기 하위 카테고리 → 전자기기를 스마트폰 하위로 이동
        Category category = Category.root("전자기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        Category newParent = Category.child(categoryId, "스마트폰", 0);
        ReflectionTestUtils.setField(newParent, "id", newParentId);

        CategoryMoveRequest request = new CategoryMoveRequest(newParentId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.of(newParent));

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_CIRCULAR_REFERENCE.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 최대 깊이 초과")
    void moveCategory_fail_maxDepthExceeded() {
        // given
        Long categoryId = 1L;
        Long newParentId = 2L;
        Long childId = 3L;
        Long grandChildId = 4L;

        Category category = Category.root("모바일기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        Category newParent = Category.root("전자기기");
        ReflectionTestUtils.setField(newParent, "id", newParentId);

        Category child = Category.child(categoryId, "스마트폰", 0);
        ReflectionTestUtils.setField(child, "id", childId);

        Category grandChild = Category.child(childId, "아이폰", 1);
        ReflectionTestUtils.setField(grandChild, "id", grandChildId);

        CategoryMoveRequest request = new CategoryMoveRequest(newParentId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.of(newParent));
        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of(child));
        given(categoryRepository.findAllByParentId(childId)).willReturn(List.of(grandChild));
        given(categoryRepository.findAllByParentId(grandChildId)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.CATEGORY_MAX_DEPTH_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 새 부모 아래 중복")
    void moveCategory_fail_childDuplicated() {
        // given
        Long categoryId = 1L;
        Long newParentId = 2L;

        Category category = Category.root("모바일기기");
        ReflectionTestUtils.setField(category, "id", categoryId);

        Category newParent = Category.root("전자기기");
        ReflectionTestUtils.setField(newParent, "id", newParentId);

        CategoryMoveRequest request = new CategoryMoveRequest(newParentId);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.of(newParent));
        given(categoryRepository.findAllByParentId(categoryId)).willReturn(List.of());
        given(categoryRepository.existsByParentIdAndName(newParentId, "모바일기기")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryAdminService.moveCategory(categoryId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CategoryErrorEnum.DUPLICATED_CATEGORY.getMessage());
    }
}
