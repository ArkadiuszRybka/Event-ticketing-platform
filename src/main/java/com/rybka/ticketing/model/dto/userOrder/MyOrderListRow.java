package com.rybka.ticketing.model.dto.userOrder;

import com.rybka.ticketing.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface MyOrderListRow {
    Long getOrderId();
    OrderStatus getStatus();
    Instant getCreatedAt();

    Long getEventId();
    String getEventTitle();
    Instant getEventStartAt();
    String getVenueName();

    Long getItemsCount(); // z COUNT(*)
    BigDecimal getTotal();
    String getCurrency();
}
