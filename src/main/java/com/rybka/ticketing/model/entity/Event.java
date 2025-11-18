package com.rybka.ticketing.model.entity;

import com.rybka.ticketing.model.enums.EventStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_name", columnList = "venue_id"),
                @Index(name = "idx_events_venue_status", columnList = "venue_id,status"),
                @Index(name = "idx_events_start", columnList = "start_at")
        }
)
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_event_venue"))
    private Venue venue;

    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, name = "start_at")
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Column(nullable = false, length = 20)
    private EventStatus status;
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
