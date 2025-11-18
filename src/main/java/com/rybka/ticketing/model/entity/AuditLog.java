package com.rybka.ticketing.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
@Table(name = "audit_log",
    indexes = @Index(name = "idx_a_time", columnList = "created_at"))
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_id")
    private Long actorId;
    @Column(name = "action", nullable = false, length = 64)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Lob
    @Column(name = "details_json")
    private String detailsJson;
}
