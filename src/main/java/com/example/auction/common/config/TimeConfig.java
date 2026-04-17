package com.example.auction.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeConfig {

    @PostConstruct
    public void setTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
