package com.rybka.ticketing.model.dto.userOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RefundRequestDto {
    private String reason; private String idempotencyKey;
}
