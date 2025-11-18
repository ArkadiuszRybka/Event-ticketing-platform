package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.entity.ReservationHold;
import com.rybka.ticketing.model.enums.HoldStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ReservationHoldRepository extends JpaRepository<ReservationHold, String> {
    Optional<ReservationHold> findByUserIdAndEvent_IdAndIdempotencyKey(Long userId, Long eventId, String idempotencyKey);

    @EntityGraph(attributePaths = {"items", "event"})
    Optional<ReservationHold> findWithItemsById(String id);

    @EntityGraph(attributePaths = {"items", "event"})
    Optional<ReservationHold> findByIdAndStatus(String id, HoldStatus status);

    Page<ReservationHold> findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(HoldStatus status, Instant before, Pageable pageable);
}
