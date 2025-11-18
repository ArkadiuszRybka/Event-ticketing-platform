package com.rybka.ticketing.service;

import com.rybka.ticketing.model.dto.order.OrderItemReadDto;
import com.rybka.ticketing.model.dto.userOrder.MyOrderListItemDto;
import com.rybka.ticketing.model.dto.userOrder.MyOrderListRow;
import com.rybka.ticketing.model.dto.userOrder.OrderDetailsDto;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.entity.Order;
import com.rybka.ticketing.model.entity.OrderItem;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.repository.EventRepository;
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
public class MyOrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private EventRepository eventRepository;

    public Page<MyOrderListItemDto> list(
            Long userId,
            Set<OrderStatus> statuses,
            Instant from,
            Instant to,
            Long eventId,
            Pageable pageable
    ) {
        boolean statusesEmpty = (statuses == null || statuses.isEmpty());
        Collection<OrderStatus> effectiveStatuses =
                statusesEmpty ? List.of(OrderStatus.PENDING) : statuses;
        Page<MyOrderListRow> page = orderRepository.searchMyOrders(
                userId,
                from,
                to,
                eventId,
                statusesEmpty,
                effectiveStatuses,
                pageable
        );

        return page.map(row -> MyOrderListItemDto.builder()
                .id(row.getOrderId())
                .status(row.getStatus())
                .createdAt(row.getCreatedAt())
                .event(new MyOrderListItemDto.EventBrief(
                        row.getEventId(),
                        row.getEventTitle(),
                        row.getEventStartAt(),
                        row.getVenueName()
                ))
                .itemsCount(row.getItemsCount() == null ? 0 : row.getItemsCount().intValue())
                .total(row.getTotal())
                .currency(row.getCurrency())
                .build());
    }

    public OrderDetailsDto getDetails(Long orderId, Long userId, boolean isAdmin) {
        Order order = isAdmin
                ? orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"))
                : orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        List<OrderItemReadDto> itemDtos = items.stream()
                .map(i -> new OrderItemReadDto(i.getRow(), i.getNumber(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());

        Event e = order.getEvent();
        String venueName = (e.getVenue() != null) ? e.getVenue().getName() : null;
        Long venueId = (e.getVenue() != null) ? e.getVenue().getId() : null;

        return OrderDetailsDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .event(new OrderDetailsDto.EventFull(
                        e.getId(), e.getTitle(), e.getStartAt(), e.getEndAt(), venueId, venueName
                ))
                .items(itemDtos)
                .total(order.getTotal())
                .currency(order.getCurrency())
                .build();
    }

}
