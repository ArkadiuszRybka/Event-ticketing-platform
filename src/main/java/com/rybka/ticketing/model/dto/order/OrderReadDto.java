package com.rybka.ticketing.model.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderReadDto(
        Long id,
        String status,
        Long eventId,
        BigDecimal total,
        String currency,
        Instant createdAt,
        List<OrderItemReadDto> items
) {
}
