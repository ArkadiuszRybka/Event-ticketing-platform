package com.rybka.ticketing.service;

import com.rybka.ticketing.config.CleanupProperties;
import com.rybka.ticketing.config.OrderProperties;
import com.rybka.ticketing.model.entity.OrderItem;
import com.rybka.ticketing.model.entity.ReservationHoldItem;
import com.rybka.ticketing.model.enums.HoldStatus;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.repository.*;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {
    private static final int MAX_RETRIES = 2;
    @Autowired
    private   CleanupProperties cleanupProps;
    @Autowired
    private OrderProperties orderProps;
    @Autowired
    private ReservationHoldRepository holdRepo;
    @Autowired
    private ReservationHoldItemRepository holdItemRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private OrderItemRepository orderItemRepo;
    @Autowired
    private EventSeatRepository seatRepo;
    @Autowired
    private AdvisoryLockService lockService;

    public void expireHoldsBatch(){
        if(cleanupProps.isDistributedLockEnabled()) {
            if(!lockService.tryAcquire(cleanupProps.getKeyHolds())) {
                log.debug("[scheduler] skip expireHoldsBatch - lock taken");
                return;
            }
        }

        try{
            int processedTotal = 0;
            while (true){
                var page = holdRepo.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        HoldStatus.ACTIVE,
                        Instant.now(),
                        PageRequest.of(0, cleanupProps.getBatchSize())
                );
                if(page.isEmpty()) break;

                for(var hold : page.getContent()) {
                    try {
                        expireSingleHold(hold.getId());
                        processedTotal++;
                    } catch (Exception e) {
                        log.warn("[scheduler] expire hold {} failed {}", hold.getId(), e.toString());
                    }
                }
                if(page.getNumberOfElements() < cleanupProps.getBatchSize()) break;
            }
            if(processedTotal > 0) {
                log.info("[scheduler expired holds: {}", processedTotal);
            }
        } finally {
            if (cleanupProps.isDistributedLockEnabled()) {
                lockService.release(cleanupProps.getKeyHolds());
            }
        }
    }

    @Transactional
    protected void expireSingleHold(String holdId){
        var holdOpt = holdRepo.findByIdAndStatus(holdId, HoldStatus.ACTIVE);
        if(holdOpt.isEmpty()) return;

        var hold = holdOpt.get();
        if(hold.getStatus() != HoldStatus.ACTIVE) return;

        var items = hold.getItems();
        if(items == null || items.isEmpty()){
            hold.setStatus(HoldStatus.EXPIRED);
            return;
        }

        Long eventId = hold.getEvent().getId();
        List<Integer> rows = items.stream().map(ReservationHoldItem::getRow).distinct().toList();
        List<Integer> numbers = items.stream().map(ReservationHoldItem::getNumber).distinct().toList();
        var pool = seatRepo.findPool(eventId, rows, numbers);

        var byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b)->a));

        int retries = 0;
        while (true){
            try {
                for(var i : items) {
                    var seat = byKey.get(i.getRow()+":"+i.getNumber());
                    if(seat != null && seat.getStatus() == SeatStatus.HELD) {
                        seat.setStatus(SeatStatus.FREE);
                    }
                }
                seatRepo.saveAllAndFlush(byKey.values());
                hold.setStatus(HoldStatus.EXPIRED);
                break;
            } catch (OptimisticLockException ex) {
                if(++retries > MAX_RETRIES) throw ex;
                try {
                    Thread.sleep(15L * retries);
                } catch (InterruptedException ignored) {}
                var reloaded = seatRepo.findPool(eventId, rows, numbers);
                byKey = reloaded.stream()
                        .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b)->a));
            }
        }
    }

    public void expirePendindOrderBatch() {
        if(cleanupProps.isDistributedLockEnabled()) {
            if(!lockService.tryAcquire(cleanupProps.getKeyOrders())) {
                log.debug("[scheduler] skip expirePendingOrdersBatch - lock taken");
                return;
            }
        }

        try {
            int processedTotal = 0;
            Instant threshold = Instant.now().minusSeconds(orderProps.getPendingTtlSeconds());
            while (true) {
                var page = orderRepo.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        OrderStatus.PENDING, threshold, PageRequest.of(0, cleanupProps.getBatchSize()));
                if (page.isEmpty()) break;

                for (var order : page.getContent()) {
                    try {
                        expireSingleOrder(order.getId());
                        processedTotal++;
                    } catch (Exception e) {
                        log.warn("[scheduler] expire order {} failed: {}", order.getId(), e.toString());
                    }
                }
                if (page.getNumberOfElements() < cleanupProps.getBatchSize()) break;
            }
            if (processedTotal > 0) {
                log.info("[scheduler] expired orders: {}", processedTotal);
            }
        } finally {
            if (cleanupProps.isDistributedLockEnabled()) {
                lockService.release(cleanupProps.getKeyOrders());
            }
        }
    }

    @Transactional
    protected void expireSingleOrder(Long orderId) {
        var orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty()) return;
        var order = orderOpt.get();
        if (order.getStatus() != OrderStatus.PENDING) return; // idempotencja

        var items = orderItemRepo.findByOrder_Id(orderId);
        if (items.isEmpty()) {
            order.setStatus(OrderStatus.EXPIRED);
            return;
        }

        Long eventId = order.getEvent().getId();
        List<Integer> rows = items.stream().map(OrderItem::getRow).distinct().toList();
        List<Integer> numbers = items.stream().map(OrderItem::getNumber).distinct().toList();
        var pool = seatRepo.findPool(eventId, rows, numbers);

        // jeśli cokolwiek jest SOLD (webhook doszedł), nie wygaszaj tego ordera – zostaw PENDING (polityka z opisu)
        boolean anySold = pool.stream().anyMatch(s -> s.getStatus() == SeatStatus.SOLD);
        if (anySold) {
            log.warn("[scheduler] order {} has SOLD seats – skipping expire", orderId);
            return;
        }

        var byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b)->a));

        int retries = 0;
        while (true) {
            try {
                for (var it : items) {
                    var seat = byKey.get(it.getRow()+":"+it.getNumber());
                    if (seat != null && seat.getStatus() == SeatStatus.LOCKED) {
                        seat.setStatus(SeatStatus.FREE);
                    }
                }
                seatRepo.saveAllAndFlush(byKey.values());
                order.setStatus(OrderStatus.EXPIRED);
                break;
            } catch (OptimisticLockException ex) {
                if (++retries > MAX_RETRIES) throw ex;
                try { Thread.sleep(15L * retries); } catch (InterruptedException ignored) {}
                var reloaded = seatRepo.findPool(eventId, rows, numbers);
                byKey = reloaded.stream()
                        .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b)->a));
            }
        }
    }

}
