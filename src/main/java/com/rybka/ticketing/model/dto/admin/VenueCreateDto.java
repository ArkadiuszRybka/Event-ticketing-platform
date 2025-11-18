package com.rybka.ticketing.model.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VenueCreateDto(
        @NotBlank String name,
        @Min(value = 1, message = "Rows must be >= 1") @Max(200) int rows,
        @Min(value = 1, message = "SeatsPerRow must be >= 1") @Max(200) int seatsPerRow
) {
}
