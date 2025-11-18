package com.rybka.ticketing.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "venues",
        uniqueConstraints = @UniqueConstraint(name = "uk_venues_name", columnNames = "name"),
        indexes = @Index(name = "idx_venues_namem", columnList = "name")
)
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private int rows;
    @Column(nullable = false)
    private int seatsPerRow;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    public int getTotalSeats(){
        return rows*seatsPerRow;
    }
}
