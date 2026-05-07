package com.example.auction.common.config.postgresql;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL 전용 커스텀 Dialect.
 *
 * Hibernate의 JPQL/HQL에는 저희가 필요한 PostgreSQL 기능을 표현 할 문법이 없습니다.
 *
 * 그래서 이 클래스를 통해 JPQL 문법을 확장합니다.
 *
 */
public class CustomPostgreSqlDialect extends PostgreSQLDialect {
    public CustomPostgreSqlDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
        BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

        functionContributions.getFunctionRegistry().patternDescriptorBuilder(
                "match_raw_ts_query",
                "(cast(?1 as tsvector) @@ cast(?2 as tsquery))"
            )
            .setExactArgumentCount(2)
            .setParameterTypes(FunctionParameterType.ANY, FunctionParameterType.STRING)
            .setInvariantType(basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN))
            .register();

        functionContributions.getFunctionRegistry().patternDescriptorBuilder(
                    "rank_raw_ts_query",
                    "ts_rank(cast(?1 as tsvector), cast(?2 as tsquery))"
            )
            .setExactArgumentCount(2)
            .setParameterTypes(FunctionParameterType.ANY, FunctionParameterType.STRING)
            .setInvariantType(basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE))
            .register();
    }
}
