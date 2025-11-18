package com.rybka.ticketing.model.dto.userOrder;

import com.rybka.ticketing.model.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyOrderListItemDto {
    private Long id;
    private OrderStatus status;
    private Instant createdAt;
    private EventBrief event;
    private int itemsCount;
    private BigDecimal total;
    private String currency;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EventBrief{
        private Long id;
        private String title;
        private Instant startAt;
        private String venueName;
    }
}
