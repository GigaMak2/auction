package com.example.auction.common.config.postgresql;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

// Hibernate JPQL/HQL로 표현할 수 없는 PostgreSQL 전용 기능을 위한 커스텀 Dialect
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
