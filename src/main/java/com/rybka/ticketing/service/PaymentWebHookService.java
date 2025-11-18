package com.rybka.ticketing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rybka.ticketing.config.HmacSigner;
import com.rybka.ticketing.config.PaymentWebHookProperties;
import com.rybka.ticketing.model.dto.payment.PaymentWebHookRequest;
import com.rybka.ticketing.model.entity.EventSeat;
import com.rybka.ticketing.model.entity.Order;
import com.rybka.ticketing.model.entity.OrderItem;
import com.rybka.ticketing.model.entity.Payment;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.PaymentStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.OrderItemRepository;
import com.rybka.ticketing.repository.OrderRepository;
import com.rybka.ticketing.repository.PaymentRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentWebHookService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventSeatRepository eventSeatRepository;
    private final PaymentWebHookProperties props;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;

    public record WebhookResult(String message){}

    public WebhookResult handle(String rawBody, String signatureHeader){
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid signature");
        }
        String expected = "sha256=" + HmacSigner.sha256Hex(props.getSecret(), rawBody);
        if (!constantTimeEquals(signatureHeader, expected)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid signature");
        }

        final PaymentWebHookRequest req;
        try {
            req = objectMapper.readValue(rawBody, PaymentWebHookRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid payload");
        }

        final Payment snapshot = paymentRepository.findByProviderRef(req.providerRef())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown provider"));

        // idempotencja webhooka
        if (snapshot.getStatus() != PaymentStatus.INIT) {
            return new WebhookResult("noop");
        }

        if (req.amount() != null && snapshot.getAmount().compareTo(req.amount()) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "amount mismatch");
        if (req.currency() != null && !snapshot.getCurrency().equals(req.currency()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "currency mismatch");

        String st = req.status() == null ? "" : req.status().toUpperCase();
        switch (st) {
            case "SUCCEEDED" -> succeed(snapshot.getId()); // przekazuj samo ID
            case "FAILED"    -> fail(snapshot.getId());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
        }

        return new WebhookResult("ok");
    }

    @Transactional
    public void fail(Long paymentId){
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found"));
        if (payment.getStatus() == PaymentStatus.INIT) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment); // merge/flush
        }
    }

    public void succeed(Long paymentId){
        int attempts = 0;
        while (true){
            try {
                succeedOnce(paymentId);
                return;
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex){
                if (++attempts >= MAX_RETRIES) throw ex;
                try { Thread.sleep(15L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Transactional
    protected void succeedOnce(Long paymentId){
        // 1) payment jako managed
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found"));

        // idempotencja w transakcji
        if (payment.getStatus() != PaymentStatus.INIT) return;

        // 2) order jako managed
        Order order = orderRepository.findById(payment.getOrder().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);
            return;
        }

        var items = orderItemRepository.findByOrder_Id(order.getId());
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "empty order items");
        }

        List<Integer> rows = items.stream().map(OrderItem::getRow).distinct().toList();
        List<Integer> nums = items.stream().map(OrderItem::getNumber).distinct().toList();
        List<EventSeat> pool = eventSeatRepository.findPool(order.getEvent().getId(), rows, nums);

        Map<String, EventSeat> byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow() + ":" + s.getNumber(), s -> s, (a,b) -> a));

        for (var it : items) {
            var seat = byKey.get(it.getRow() + ":" + it.getNumber());
            if (seat == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "seat not found");
            if (seat.getStatus() != SeatStatus.LOCKED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "seats not locked");
            seat.setStatus(SeatStatus.SOLD);
        }

        // 3) aktualizacje domeny
        order.setStatus(OrderStatus.PAID);
        payment.setStatus(PaymentStatus.SUCCEEDED);

        // 4) zapisy
        eventSeatRepository.saveAllAndFlush(byKey.values());
        orderRepository.save(order);          // (bezpiecznie i czytelnie)
        paymentRepository.save(payment);      // kluczowe: payment był detached w wersji pierwotnej
    }

    private static boolean constantTimeEquals(String a, String b){
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}