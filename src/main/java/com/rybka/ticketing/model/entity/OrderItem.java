package com.rybka.ticketing.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_oi_order", columnList = "order_id")
        },
        uniqueConstraints = {
                @UniqueConstraint( name = "uq_oi_order_seat", columnNames = {"order_id", "row_no", "seat_no"})
        })
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Column(name = "row_no", nullable = false)
    private int row;
    @Column(name = "seat_no", nullable = false)
    private int number;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

}
