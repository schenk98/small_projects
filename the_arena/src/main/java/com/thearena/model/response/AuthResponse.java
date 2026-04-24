package com.thearena.model.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username
) {
}
