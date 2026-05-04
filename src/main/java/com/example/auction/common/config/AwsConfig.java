package com.example.auction.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

@Configuration
public class AwsConfig {

    @Bean
    public SchedulerClient schedulerClient() {
        return SchedulerClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    // S3 Presigned URL 생성기 — ECS Task Role 자격증명을 자동으로 사용
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}
