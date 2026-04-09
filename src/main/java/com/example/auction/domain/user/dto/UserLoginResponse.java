package com.example.auction.domain.user.dto;

public record UserLoginResponse(
        String accessToken,
        String refreshToken
) {}
