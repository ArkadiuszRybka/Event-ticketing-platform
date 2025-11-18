package com.rybka.ticketing.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="reservation_hold_item",
        indexes = @Index(name="idx_rhi_hold", columnList="hold_id"))
public class ReservationHoldItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="hold_id", nullable=false)
    private ReservationHold hold;

    @Column(name="row_no", nullable=false)
    private int row;

    @Column(name="seat_no", nullable=false)
    private int number;
}
