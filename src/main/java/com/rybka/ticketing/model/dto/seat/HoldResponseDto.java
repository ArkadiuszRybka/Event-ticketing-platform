package com.rybka.ticketing.model.dto.seat;

import java.time.Instant;
import java.util.List;

public record HoldResponseDto(
        String holdId,
        Long eventId,
        Instant expiresAt,
        List<HoldSeatResultDto> seats,
        Integer ttlSeconds
) {
}
