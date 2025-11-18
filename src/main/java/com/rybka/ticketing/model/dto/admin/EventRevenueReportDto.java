package com.rybka.ticketing.model.dto.admin;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class EventRevenueReportDto {
    private Long eventId;
    private String title;
    private Instant startAt;
    private String currency;
    private long ordersPaidCount;
    private long ordersRefundedCount;
    private long ticketsSold;
    private long ticketsRefunded;
    private BigDecimal grossRevenue;
    private BigDecimal refundsTotal;
    private BigDecimal netRevenue;
}
