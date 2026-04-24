package com.thearena.model.response;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {
}
