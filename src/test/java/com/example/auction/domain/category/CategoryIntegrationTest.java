package com.example.auction.domain.category;

import com.example.auction.domain.auth.dto.request.AuthLoginRequest;
import com.example.auction.domain.auth.dto.request.AuthSignupRequest;
import com.example.auction.domain.category.dto.request.CategoryCreateRequest;
import com.example.auction.domain.category.dto.request.CategoryMoveRequest;
import com.example.auction.domain.category.dto.request.CategoryRenameRequest;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.testutils.BaseIntegrationTest;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CategoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Flyway flyway;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        flyway.clean();
        flyway.migrate();

        User admin = User.of("admin@test.com", passwordEncoder.encode("password123"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);

        signup("user@test.com", "password123");

        adminToken = getAccessToken("admin@test.com", "password123");
        userToken = getAccessToken("user@test.com", "password123");
    }

    @AfterEach
    void cleanUpRedis() {
        var factory = redisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }


    // ========================
    // 카테고리 생성
    // ========================

    @Test
    @DisplayName("카테고리 생성 성공 - root")
    void createCategory_success_root() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "전자기기");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리를 생성했습니다"))
                .andExpect(jsonPath("$.data.name").value("전자기기"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist());
    }

    @Test
    @DisplayName("카테고리 생성 성공 - child")
    void createCategory_success_child() throws Exception {
        // given
        Long parentId = createCategory(null, "전자기기");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest(parentId, "스마트폰"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("스마트폰"))
                .andExpect(jsonPath("$.data.parentId").value(parentId));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - root 중복")
    void createCategory_fail_rootDuplicated() throws Exception {
        // given
        createCategory(null, "전자기기");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest(null, "전자기기"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 카테고리입니다"));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 부모 카테고리 없음")
    void createCategory_fail_parentNotFound() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(9999L, "스마트폰");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("카테고리를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 권한 없음 (일반 유저)")
    void createCategory_fail_forbidden() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(null, "전자기기");

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    // ========================
    // 카테고리 이름 수정
    // ========================

    @Test
    @DisplayName("카테고리 이름 수정 성공")
    void renameCategory_success() throws Exception {
        // given
        Long categoryId = createCategory(null, "전자기기");

        // when & then
        mockMvc.perform(patch("/api/admin/categories/" + categoryId + "/name")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRenameRequest("가전제품"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 이름을 수정했습니다"))
                .andExpect(jsonPath("$.data.name").value("가전제품"));
    }

    @Test
    @DisplayName("카테고리 이름 수정 실패 - 중복")
    void renameCategory_fail_duplicated() throws Exception {
        // given
        createCategory(null, "전자기기");
        Long categoryId = createCategory(null, "가전제품");

        // when & then
        mockMvc.perform(patch("/api/admin/categories/" + categoryId + "/name")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRenameRequest("전자기기"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 카테고리입니다"));
    }


    // ========================
    // 카테고리 이동
    // ========================

    @Test
    @DisplayName("카테고리 이동 성공")
    void moveCategory_success() throws Exception {
        // given
        Long parentId = createCategory(null, "전자기기");
        Long categoryId = createCategory(null, "스마트폰");

        // when & then
        mockMvc.perform(patch("/api/admin/categories/" + categoryId + "/parent")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryMoveRequest(parentId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리를 이동했습니다"))
                .andExpect(jsonPath("$.data.parentId").value(parentId));
    }

    @Test
    @DisplayName("카테고리 이동 실패 - 순환 참조")
    void moveCategory_fail_circularReference() throws Exception {
        // given
        // 전자기기(id=A) → 스마트폰(id=B) 트리에서 전자기기를 스마트폰 하위로 이동 시도
        Long parentId = createCategory(null, "전자기기");
        Long childId = createCategory(parentId, "스마트폰");

        // when & then
        mockMvc.perform(patch("/api/admin/categories/" + parentId + "/parent")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryMoveRequest(childId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("순환 참조가 발생하여 카테고리를 이동할 수 없습니다"));
    }


    // ========================
    // 카테고리 목록 조회
    // ========================

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 트리 구조 반환")
    void getCategoryList_success() throws Exception {
        // given
        Long parentId = createCategory(null, "전자기기");
        createCategory(parentId, "스마트폰");

        // when & then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("전자기기"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("스마트폰"));
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 카테고리 없음")
    void getCategoryList_success_empty() throws Exception {
        // when & then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }


    // ========================
    // 헬퍼 메서드
    // ========================

    private void signup(String email, String password) throws Exception {
        AuthSignupRequest request = new AuthSignupRequest(email, password);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String getAccessToken(String email, String password) throws Exception {
        AuthLoginRequest request = new AuthLoginRequest(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asString();
    }

    private Long createCategory(Long parentId, String name) throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest(parentId, name);
        String response = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("categoryId").asLong();
    }
}
