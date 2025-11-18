package com.rybka.ticketing.model.dto.auth;

import com.rybka.ticketing.model.enums.Role;

import java.time.LocalDateTime;

public record MeReadDto(
        Long id,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
