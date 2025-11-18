package com.rybka.ticketing.model.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventReadDto(
        Long id,
        Long venueId,
        String venueName,
        String title,
        Instant startAt,
        Instant endAt,
        String status,
        BigDecimal basePrice
) {
}
