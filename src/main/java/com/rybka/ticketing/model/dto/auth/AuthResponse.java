package com.rybka.ticketing.model.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
