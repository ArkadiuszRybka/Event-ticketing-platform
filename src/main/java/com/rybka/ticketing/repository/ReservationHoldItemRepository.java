package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.entity.ReservationHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationHoldItemRepository extends JpaRepository<ReservationHoldItem, Long> {
    List<ReservationHoldItem> findByHold_Id(String holdId);
}
