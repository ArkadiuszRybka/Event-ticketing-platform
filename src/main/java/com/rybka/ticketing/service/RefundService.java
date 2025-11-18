package com.rybka.ticketing.service;

import com.rybka.ticketing.model.dto.userOrder.RefundResponseDto;
import com.rybka.ticketing.model.entity.*;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.repository.AuditLogRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.OrderItemRepository;
import com.rybka.ticketing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {
    private static final int MAX_RETRIES = 3;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public RefundResponseDto refundOrder(Long adminId, Long orderId, String reason, String idempotencyKey){
        if(reason == null || reason.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: reason required");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        if(order.getStatus() == OrderStatus.REFUNDED){
            return RefundResponseDto.builder()
                    .orderId(order.getId())
                    .previousStatus(OrderStatus.REFUNDED.name())
                    .newStatus(OrderStatus.REFUNDED.name())
                    .applied(false)
                    .seatsRestocked(false)
                    .build();
        }

        if(order.getStatus() != OrderStatus.PAID)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "invalid_order_state");

        Event event = order.getEvent();
        boolean beforeEvent = Instant.now().isBefore(Instant.from(event.getStartAt()));

        boolean seatsRestocked = false;
        if (beforeEvent){
            int attempts = 0;
            while (true){
                try {
                    seatsRestocked = restockSeats(order);
                    break;
                } catch (ObjectOptimisticLockingFailureException ex){
                    if(++attempts > MAX_RETRIES) throw ex;
                    try {
                        Thread.sleep(20L * attempts);
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        OrderStatus prev = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundAt(Instant.now());
        order.setRefundReason(reason);
        orderRepository.save(order);

        AuditLog log = AuditLog.builder()
                .actorId(adminId)
                .action("ORDER_REFUND")
                .entityType("Order")
                .entityId(order.getId())
                .createdAt(Instant.now())
                .detailsJson("{\"reason\":\""+escape(reason)+"\",\"seatsRestocked\":"+seatsRestocked+"}")
                .build();

        return RefundResponseDto.builder()
                .orderId(order.getId())
                .previousStatus(prev.name())
                .newStatus(order.getStatus().name())
                .seatsRestocked(seatsRestocked)
                .applied(true)
                .build();
    }

    private String escape(String s){ return s==null?null:s.replace("\"","\\\""); }

    @Transactional
    protected boolean restockSeats(Order order){
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        if(items.isEmpty()) return false;

        Long eventId = order.getEvent().getId();
        List<Integer> rows = items.stream().map(OrderItem::getRow).distinct().toList();
        List<Integer> nums = items.stream().map(OrderItem::getNumber).distinct().toList();

        List<EventSeat> pool = eventSeatRepository.findPool(eventId, rows, nums);
        Map<String, EventSeat> byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b) -> a));

        boolean any = false;
        for(OrderItem it : items){
            EventSeat seat = byKey.get(it.getRow()+":"+ it.getNumber());
            if(seat != null && seat.getStatus() == SeatStatus.SOLD){
                seat.setStatus(SeatStatus.FREE);
                any = true;
            }
        }
        if (any) eventSeatRepository.saveAllAndFlush(byKey.values());
        return any;
    }

}
