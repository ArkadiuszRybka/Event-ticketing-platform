package com.rybka.ticketing.model.dto.admin;

import java.time.LocalDateTime;

public record VenueReadDto(
        Long id,
        String name,
        int rows,
        int seatsPerRows,
        int totalSeats,
        LocalDateTime createdAt
) {
}
