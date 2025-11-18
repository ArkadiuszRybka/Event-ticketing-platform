package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderRef(String providerRef);
    Optional<Payment> findByOrder_IdAndIdempotencyKey(Long orderId, String idempotencyKey);
}
