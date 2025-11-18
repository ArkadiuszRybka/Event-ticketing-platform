package com.rybka.ticketing.model.dto.publics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventDetailsReadDto(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        Long venueId,
        String venueName,
        int rows,
        int seatsPerRow,
        int totalSeats,
        BigDecimal basePrice,
        SeatsSummary seatsSummary
) {
    public record SeatsSummary(long free, long held, long locked, long sold){}
}
