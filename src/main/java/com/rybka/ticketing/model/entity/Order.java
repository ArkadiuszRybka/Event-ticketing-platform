package com.rybka.ticketing.model.entity;

import com.rybka.ticketing.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_o_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_o_user_status_created", columnList = "user_id, status, created_at DESC"),
                @Index(name = "idx_o_status_created", columnList = "status, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_o_user_key", columnNames = {"user_id", "idempotency_key"})
        })
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status;
    @Column(name = "total", nullable = false, length = 16)
    private BigDecimal total;
    @Column(name = "currency", nullable = false, precision = 12, scale = 12)
    private String currency;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "refunded_at")
    private Instant refundAt;
    @Column(name = "refund_reason", length = 256)
    private String refundReason;
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
