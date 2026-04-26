package com.example.auction.testutils;

import com.redis.testcontainers.RedisContainer;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * pgvector와 Redis를 Testcontainers로 실행하는 통합 테스트 기반 클래스입니다.
 *
 * <p>컨테이너는 테스트 실행 시 한 번만 시작되며, 모든 하위 클래스가 공유합니다.
 *
 * <p>다른 설정이 필요한 경우, 이 클래스를 상속한 하위 클래스에서 설정을 변경하려고 하지 마세요.
 * 컨테이너는 모든 하위 클래스가 공유하므로, 하위 클래스에서의 설정 변경은
 * 다른 테스트 클래스에도 영향을 미칠 수 있습니다.
 *
 * <p>다른 인프라 설정이 필요하다면, 이 클래스를 복붙해서 새로운 기반 클래스를 만드세요.
 * (아니면 Testcontainers를 공부해서 테스트 별로 container를 띄우는 방법을 알아보세요)
 */
public abstract class BaseIntegrationTest {
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17")
                .asCompatibleSubstituteFor("postgres")
    );

    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:8.6.2")
    );

    static  {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }
}
