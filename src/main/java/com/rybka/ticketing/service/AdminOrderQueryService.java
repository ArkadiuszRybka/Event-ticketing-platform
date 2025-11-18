package com.rybka.ticketing.service;

import com.rybka.ticketing.model.dto.admin.AdminOrderDetailsDto;
import com.rybka.ticketing.model.dto.admin.AdminOrderListItemDto;
import com.rybka.ticketing.model.dto.admin.AdminOrderListRow;
import com.rybka.ticketing.model.dto.order.OrderItemReadDto;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.entity.Order;
import com.rybka.ticketing.model.entity.OrderItem;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.repository.OrderItemRepository;
import com.rybka.ticketing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderQueryService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    public Page<AdminOrderListItemDto> list(
            Set<OrderStatus> statuses,
            String userEmail,
            Long eventId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        boolean statusesEmpty = (statuses == null || statuses.isEmpty());
        Collection<OrderStatus> effective = statusesEmpty ? List.of(OrderStatus.PAID) : statuses;

        Page<AdminOrderListRow> page = orderRepository.adminSearchOrders(
                from, to, eventId, userEmail, statusesEmpty, effective, pageable
        );
        return page.map(r -> AdminOrderListItemDto.builder()
                .id(r.getOrderId())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .userEmail(r.getUserEmail())
                .event(new AdminOrderListItemDto.EventBrief(
                        r.getEventId(), r.getEventTitle(), r.getEventStartAt(), r.getVenueName()
                ))
                .itemsCount(r.getItemsCount() == null ? 0 : r.getItemsCount().intValue())
                .total(r.getTotal())
                .currency(r.getCurrency())
                .build());
    }

    public AdminOrderDetailsDto getDetails(Long orderId){
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        List<OrderItem> items = orderItemRepository.findByOrder_Id(o.getId());
        List<OrderItemReadDto> itemDtos = items.stream()
                .map(i -> new OrderItemReadDto(i.getRow(), i.getNumber(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());

        Event e = o.getEvent();
        String venueName = e.getVenue() != null ? e.getVenue().getName() : null;

        return AdminOrderDetailsDto.builder()
                .id(o.getId())
                .status(o.getStatus().name())
                .createdAt(o.getCreatedAt())
                .user(new AdminOrderDetailsDto.UserBrief(o.getUserId(), null))
                .event(new AdminOrderDetailsDto.EventFull(
                        e.getId(), e.getTitle(), e.getStartAt(), e.getEndAt(), venueName
                ))
                .items(itemDtos)
                .total(o.getTotal())
                .currency(o.getCurrency())
                .refundAt(o.getRefundAt())
                .refundReason(o.getRefundReason())
                .payment(new AdminOrderDetailsDto.PaymentMeta(null, null))
                .build();
    }
}
