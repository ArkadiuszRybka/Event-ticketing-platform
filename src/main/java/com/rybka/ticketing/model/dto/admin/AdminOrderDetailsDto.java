package com.rybka.ticketing.model.dto.admin;

import com.rybka.ticketing.model.dto.order.OrderItemReadDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class AdminOrderDetailsDto {
    private Long id;
    private String status;
    private Instant createdAt;
    private UserBrief user;
    private EventFull event;
    private List<OrderItemReadDto> items;
    private BigDecimal total;
    private String currency;
    private Instant refundAt;
    private String refundReason;
    private PaymentMeta payment;

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class UserBrief { public Long id; public String email; }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class EventFull {
        public Long id; public String title; public Instant startAt; public Instant endAt; public String venueName;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class PaymentMeta { public String lastStatus; public String providerRef; }
}
