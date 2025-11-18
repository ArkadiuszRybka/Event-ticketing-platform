package com.rybka.ticketing.model.dto.admin;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class AdminOrderListItemDto {
    private Long id;
    private String status;
    private Instant createdAt;
    private String userEmail;
    private EventBrief event;
    private int itemsCount;
    private BigDecimal total;
    private String currency;

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class EventBrief {
        public Long id; public String title; public Instant startAt; public String venueName;
    }
}