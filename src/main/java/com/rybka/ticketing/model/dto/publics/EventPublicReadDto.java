package com.rybka.ticketing.model.dto.publics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventPublicReadDto(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        Long venueId,
        String venueName,
        BigDecimal basePrice
) {
}
