package com.rybka.ticketing.model.dto.userOrder;

import com.rybka.ticketing.model.dto.order.OrderItemReadDto;
import com.rybka.ticketing.model.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class OrderDetailsDto {
    private Long id;
    private OrderStatus status;
    private Instant createdAt;

    private EventFull event;
    private List<OrderItemReadDto> items;

    private BigDecimal total;
    private String currency;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EventFull {
        private Long id;
        private String title;
        private Instant startAt;
        private Instant endAt;
        private Long venueId;
        private String venueName;
    }
}
