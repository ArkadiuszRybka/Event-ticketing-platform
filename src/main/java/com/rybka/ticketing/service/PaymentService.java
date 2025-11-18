package com.rybka.ticketing.service;

import com.rybka.ticketing.model.dto.payment.PaymentInitRequest;
import com.rybka.ticketing.model.dto.payment.PaymentInitResposne;
import com.rybka.ticketing.model.entity.Order;
import com.rybka.ticketing.model.entity.Payment;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.PaymentStatus;
import com.rybka.ticketing.repository.OrderRepository;
import com.rybka.ticketing.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;

    private Long currentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return uid;
    }

    @Transactional
    public PaymentInitResposne init(PaymentInitRequest req, String idempotencyKey){
        if (req == null || req.orderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: orderId required");
        }
        Long uid = currentUserId();

        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        if(!order.getUserId().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }

        if(order.getStatus() != OrderStatus.PENDING){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "invalid order state");
        }

        if(idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByOrder_IdAndIdempotencyKey(order.getId(), idempotencyKey);
            if(existing.isPresent()){
                var p = existing.get();
                return new PaymentInitResposne(
                        p.getId(), p.getProviderRef(), p.getStatus().name(), p.getAmount(), p.getCurrency()
                );
            }
        }

        Payment p = new Payment();
        p.setOrder(order);
        p.setStatus(PaymentStatus.INIT);
        p.setAmount(order.getTotal());
        p.setCurrency(order.getCurrency());
        p.setProviderRef("PAY_" + UUID.randomUUID());
        p.setIdempotencyKey(idempotencyKey);

        paymentRepository.save(p);

        return new PaymentInitResposne(
                p.getId(), p.getProviderRef(), p.getStatus().name(), p.getAmount(), p.getCurrency()
        );
    }
}
