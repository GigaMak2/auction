package com.example.auction.domain.auth.dto.response;

public record AuthLoginResponse(
        String accessToken,
        String refreshToken
) {}
