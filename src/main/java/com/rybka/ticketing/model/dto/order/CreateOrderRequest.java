package com.rybka.ticketing.model.dto.order;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String holdId,
        String buyerEmail,
        String firstName,
        String lastName
) {
}
