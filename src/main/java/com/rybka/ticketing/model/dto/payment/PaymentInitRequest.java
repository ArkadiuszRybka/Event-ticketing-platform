package com.rybka.ticketing.model.dto.payment;


public record PaymentInitRequest(
        Long orderId,
        String method
) {
}
