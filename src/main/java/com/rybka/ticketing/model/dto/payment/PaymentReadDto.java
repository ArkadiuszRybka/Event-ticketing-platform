package com.rybka.ticketing.model.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReadDto(
        Long id,
        Long orderId,
        String providerRef,
        String status,
        BigDecimal amount,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
}
