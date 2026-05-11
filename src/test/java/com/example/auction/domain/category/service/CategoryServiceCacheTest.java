package com.example.auction.domain.category.service;

import com.example.auction.domain.category.dto.request.CategoryCreateRequest;
import com.example.auction.domain.category.dto.request.CategoryMoveRequest;
import com.example.auction.domain.category.dto.request.CategoryRenameRequest;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CategoryService.class, CategoryAdminService.class, CategoryServiceCacheTest.TestConfig.class})
public class CategoryServiceCacheTest {

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryAdminService categoryAdminService;

    @Autowired
    private CacheManager cacheManager;

    @TestConfiguration
    @EnableCaching
    static class TestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("getCategoryList");
        }
    }

    @BeforeEach
    void setUp() {
        // @Transactional 처리용
        given(transactionManager.getTransaction(any())).willReturn(mock(TransactionStatus.class));
        cacheManager.getCache("getCategoryList").clear();
    }


    // ========================
    // @Cacheable 검증
    // ========================

    @Test
    @DisplayName("getCategoryList - 동일 호출 2번 시 repository는 1번만 호출")
    void getCategoryList_cacheable() {
        // given
        Category root = Category.root("전자기기");
        ReflectionTestUtils.setField(root, "id", 1L);
        given(categoryRepository.findAll()).willReturn(List.of(root));

        // when
        categoryService.getCategoryList();
        categoryService.getCategoryList(); // 캐시 히트

        // then
        verify(categoryRepository, times(1)).findAll();
    }


    // ========================
    // @CacheEvict 검증
    // ========================

    @Test
    @DisplayName("createCategory - 캐시 무효화 후 재조회 시 repository 재호출")
    void createCategory_evictsCache() {
        // given
        Category root = Category.root("전자기기");
        ReflectionTestUtils.setField(root, "id", 1L);
        given(categoryRepository.findAll()).willReturn(List.of(root));
        categoryService.getCategoryList();

        // when
        given(categoryRepository.existsByParentIdIsNullAndName("가구")).willReturn(false);
        given(categoryRepository.save(any())).willAnswer(inv -> {
            Category c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 2L);
            return c;
        });
        categoryAdminService.createCategory(new CategoryCreateRequest(null, "가구"));
        categoryService.getCategoryList(); // 무효화 후 재조회

        // then
        verify(categoryRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("renameCategory - 캐시 무효화 후 재조회 시 repository 재호출")
    void renameCategory_evictsCache() {
        // given
        Category root = Category.root("전자기기");
        ReflectionTestUtils.setField(root, "id", 1L);
        given(categoryRepository.findAll()).willReturn(List.of(root));
        categoryService.getCategoryList();

        // when
        given(categoryRepository.findById(1L)).willReturn(Optional.of(root));
        given(categoryRepository.existsByParentIdIsNullAndName("가전제품")).willReturn(false);
        categoryAdminService.renameCategory(1L, new CategoryRenameRequest("가전제품"));
        categoryService.getCategoryList();

        // then
        verify(categoryRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("moveCategory - 캐시 무효화 후 재조회 시 repository 재호출")
    void moveCategory_evictsCache() {
        // given
        Category root = Category.root("전자기기");
        ReflectionTestUtils.setField(root, "id", 1L);
        given(categoryRepository.findAll()).willReturn(List.of(root));
        categoryService.getCategoryList();

        // when
        Category child = Category.child(1L, "스마트폰", 0);
        ReflectionTestUtils.setField(child, "id", 2L);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(child));
        given(categoryRepository.existsByParentIdIsNullAndName("스마트폰")).willReturn(false);
        given(categoryRepository.findAllByParentId(2L)).willReturn(List.of());
        categoryAdminService.moveCategory(2L, new CategoryMoveRequest(null));
        categoryService.getCategoryList();

        // then
        verify(categoryRepository, times(2)).findAll();
    }
}
