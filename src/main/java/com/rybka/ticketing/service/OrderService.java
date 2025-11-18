package com.rybka.ticketing.service;

import com.rybka.ticketing.config.OrderProperties;
import com.rybka.ticketing.model.entity.*;
import com.rybka.ticketing.model.enums.EventStatus;
import com.rybka.ticketing.model.enums.HoldStatus;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.model.dto.order.OrderItemReadDto;
import com.rybka.ticketing.model.dto.order.OrderReadDto;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.OrderRepository;
import com.rybka.ticketing.repository.ReservationHoldRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ReservationHoldRepository reservationHoldRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private OrderProperties orderProps;

    public OrderReadDto createFromHold(Long userId, String holdId, String idempotencyKey){
        if(holdId == null || holdId.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: holdId required");
        }

        if(idempotencyKey != null && !idempotencyKey.isBlank()){
            var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if(existing.isPresent()){
                return toDto(existing.get());
            }
        }

        ReservationHold hold = reservationHoldRepository.findWithItemsById(holdId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hold not found"));

        if(!Objects.equals(hold.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }

        Instant now = Instant.now();
        if(hold.getExpiresAt().isBefore(now) || hold.getExpiresAt().equals(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "hold expired");
        }

        if(hold.getStatus() != HoldStatus.ACTIVE) {
            if(hold.getStatus() == HoldStatus.CONSUMED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "already consumed");
            }
            if(hold.getStatus() == HoldStatus.EXPIRED){
                throw new ResponseStatusException(HttpStatus.GONE, "hold expired");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "invalid hold state");
        }

        Event event = eventRepository.findById(hold.getEvent().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));

        if(event.getStatus() != EventStatus.PUBLISHED){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "event cancelled");
        }

        int attempts = 0;
        while (true){
            try {
                return doCreateOrderTx(userId, idempotencyKey, hold, event);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                if(++attempts >= MAX_RETRIES) throw ex;
                try { Thread.sleep(20L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Transactional
    protected OrderReadDto doCreateOrderTx(Long userId, String idempotencyKey, ReservationHold hold, Event event) {
        Long eventId = event.getId();

        var reqSeats = hold.getItems();
        if(reqSeats.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: hold has no seats");
        }

        List<Integer> rows = reqSeats.stream().map(ReservationHoldItem::getRow).distinct().toList();
        List<Integer> numbers = reqSeats.stream().map(ReservationHoldItem::getNumber).distinct().toList();
        List<EventSeat> pool = eventSeatRepository.findPool(eventId, rows, numbers);

        Map<String, EventSeat> byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b)->a ));

        List<String> notHeld = new ArrayList<>();
        for (ReservationHoldItem i : reqSeats){
            EventSeat seat = byKey.get(i.getRow()+":"+i.getNumber());
            if(seat == null){
                notHeld.add(i.getRow()+":"+i.getNumber());
                continue;
            }
            if(seat.getStatus() != SeatStatus.HELD){
                notHeld.add(i.getRow()+":"+i.getNumber()+"("+seat.getStatus()+")");
            }
        }

        if(!notHeld.isEmpty()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "seats not heldL " + notHeld);
        }

        for (ReservationHoldItem i : reqSeats){
            EventSeat seat = byKey.get(i.getRow()+":"+i.getNumber());
            seat.setStatus(SeatStatus.LOCKED);
        }
        eventSeatRepository.saveAllAndFlush(byKey.values());

        BigDecimal unit = event.getBasePrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(reqSeats.size())).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setUserId(userId);
        order.setEvent(event);
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency(orderProps.getCurrency());
        order.setTotal(total);
        order.setCreatedAt(Instant.now());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            order.setIdempotencyKey(idempotencyKey);
        }

        List<OrderItem> items = new ArrayList<>(reqSeats.size());
        for (ReservationHoldItem i : reqSeats) {
            items.add(OrderItem.builder()
                    .order(order)
                    .row(i.getRow())
                    .number(i.getNumber())
                    .unitPrice(unit)
                    .lineTotal(unit)
                    .build());
        }
        order.setItems(items);

        orderRepository.save(order);

        hold.setStatus(HoldStatus.CONSUMED);
        reservationHoldRepository.save(hold);

        return toDto(order);
    }

    private OrderReadDto toDto(Order o){
        List<OrderItem> srcItems = (o.getItems() == null) ? Collections.emptyList() : o.getItems();
        List<OrderItemReadDto> items = srcItems.stream()
                .map(i -> new OrderItemReadDto(i.getRow(), i.getNumber(), i.getUnitPrice(), i.getLineTotal()))
                .toList();

        return new OrderReadDto(
                o.getId(),
                o.getStatus().name(),
                o.getEvent().getId(),
                o.getTotal(),
                o.getCurrency(),
                o.getCreatedAt(),
                items
        );
    }

    public OrderReadDto getForUser(Long orderId, Long requestedId, boolean requstedisAdmin){
        var o = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
        if(!requstedisAdmin && !o.getUserId().equals(requestedId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return toDto(o);
    }

}
