package com.rybka.ticketing.model.dto.payment;

import java.math.BigDecimal;

public record PaymentInitResposne(
        Long paymentId,
        String providerRef,
        String status,
        BigDecimal amount,
        String currency
) {
}
