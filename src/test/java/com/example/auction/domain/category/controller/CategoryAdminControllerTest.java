package com.example.auction.domain.category.controller;

import com.example.auction.common.exception.GlobalExceptionHandler;
import com.example.auction.domain.category.dto.*;
import com.example.auction.domain.category.service.CategoryAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CategoryAdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
public class CategoryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryAdminService categoryAdminService;


    // ========================
    // 카테고리 생성
    // ========================

    @Test
    @DisplayName("카테고리 생성 성공")
    void createCategory_success() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "전자기기");
        CategoryCreateResponse response = new CategoryCreateResponse(1L, null, "전자기기", LocalDateTime.now());

        given(categoryAdminService.createCategory(any(CategoryCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 생성 요청 성공"))
                .andExpect(jsonPath("$.data.categoryId").value(1L))
                .andExpect(jsonPath("$.data.name").value("전자기기"))
                .andDo(document("category-admin/create-category",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - name 공백")
    void createCategory_fail_nameBlank() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리 이름을 입력해주세요"));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - parentId 0 이하")
    void createCategory_fail_parentIdNotPositive() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(0L, "스마트폰");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리 아이디는 1 이상이어야 합니다"));
    }


    // ========================
    // 카테고리 이름 수정
    // ========================

    @Test
    @DisplayName("카테고리 이름 수정 성공")
    void renameCategory_success() throws Exception {
        // given
        CategoryRenameRequest request = new CategoryRenameRequest("가전제품");
        CategoryRenameResponse response = new CategoryRenameResponse(1L, "가전제품", LocalDateTime.now());

        given(categoryAdminService.renameCategory(eq(1L), any(CategoryRenameRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/admin/categories/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 이름 수정 요청 성공"))
                .andExpect(jsonPath("$.data.categoryId").value(1L))
                .andExpect(jsonPath("$.data.name").value("가전제품"))
                .andDo(document("category-admin/rename-category",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - name 공백")
    void renameCategory_fail_nameBlank() throws Exception {
        // given
        CategoryRenameRequest request = new CategoryRenameRequest("");

        // when & then
        mockMvc.perform(patch("/api/admin/categories/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리 이름을 입력해주세요"));
    }


    // ========================
    // 카테고리 이동
    // ========================

    @Test
    @DisplayName("카테고리 이동 성공")
    void moveCategory_success() throws Exception {
        // given
        CategoryMoveRequest request = new CategoryMoveRequest(2L);
        CategoryMoveResponse response = new CategoryMoveResponse(1L, 2L, "스마트폰", LocalDateTime.now());

        given(categoryAdminService.moveCategory(eq(1L), any(CategoryMoveRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/admin/categories/1/parent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 이동 요청 성공"))
                .andExpect(jsonPath("$.data.categoryId").value(1L))
                .andExpect(jsonPath("$.data.parentId").value(2L))
                .andDo(document("category-admin/move-category",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("카테고리 이동 실패 - parentId 0 이하")
    void moveCategory_fail_parentIdNotPositive() throws Exception {
        // given
        CategoryMoveRequest request = new CategoryMoveRequest(0L);

        // when & then
        mockMvc.perform(patch("/api/admin/categories/1/parent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리 아이디는 1 이상이어야 합니다"));
    }
}
