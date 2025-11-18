package com.rybka.ticketing.model.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

public interface AdminOrderListRow {
    Long getOrderId();
    String getStatus();
    Instant getCreatedAt();
    String getUserEmail();
    Long getEventId();
    String getEventTitle();
    Instant getEventStartAt();
    String getVenueName();
    Long getItemsCount();
    BigDecimal getTotal();
    String getCurrency();
}
