package com.rybka.ticketing.model.dto.userOrder;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundResponseDto {
    private Long orderId;
    private String previousStatus;
    private String newStatus;
    private boolean seatsRestocked;
    private boolean applied;
}
