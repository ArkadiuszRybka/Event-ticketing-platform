package com.rybka.ticketing.model.dto.order;

import java.math.BigDecimal;

public record OrderItemReadDto(
        int row,
        int number,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
