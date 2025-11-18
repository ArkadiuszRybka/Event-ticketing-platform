package com.rybka.ticketing.model.entity;

import com.rybka.ticketing.model.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(
        name = "reservation_hold",
        indexes = {
                @Index(name="idx_rh_event", columnList="event_id"),
                @Index(name="idx_rh_user", columnList="user_id"),
                @Index(name="idx_rh_expires", columnList="expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rh_user_event_key", columnNames = {"user_id", "event_id", "idempotency_key"})
        }
)
public class ReservationHold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="event_id", nullable=false)
    private Event event;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=16)
    private HoldStatus status;

    @Column(name="idempotency_key", nullable=false, length=64)
    private String idempotencyKey;

    @OneToMany(mappedBy = "hold", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationHoldItem> items = new ArrayList<>();
}
