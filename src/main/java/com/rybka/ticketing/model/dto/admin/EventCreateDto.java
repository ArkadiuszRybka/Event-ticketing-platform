package com.rybka.ticketing.model.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventCreateDto(
        @NotNull Long venueId,
        @NotBlank String title,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @NotNull @DecimalMin(value = "0.01") BigDecimal basePrice
        ) {
}
