package com.rybka.ticketing.model.entity;

import com.rybka.ticketing.model.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "event_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_eventseat_event_row_number",
                columnNames = {"event_id", "row_number", "seat_number"}
        ),
        indexes = {
                @Index(name = "idx_eventseat_event", columnList = "event_id"),
                @Index(name = "idx_eventseat_event_status", columnList = "event_id, status")
        }
)
public class EventSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_eventseat_event"))
    private Event event;
    @Column(name = "row_number", nullable = false)
    private int row;
    @Column(name = "seat_number", nullable = false)
    private int number;
    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false, length = 10)
    private SeatStatus status;
    @Version
    @Column(nullable = false)
    private int version;
}
