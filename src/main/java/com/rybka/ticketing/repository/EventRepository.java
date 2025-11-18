package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.enums.EventStatus;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByVenue_Id(Long id, Pageable pageable);
    boolean existsByVenue_Id(Long id);
    @Query("""
        select (count(e) > 0) from Event e
        where e.venue.id = :venueId
          and e.status    = :status
          and e.startAt   < :endAt
          and e.endAt     > :startAt
    """)
    boolean existsOverlapVenue(@Param("venueId") Long venueId,
                               @Param("status") EventStatus status,
                               @Param("startAt") Instant startAt,
                               @Param("endAt") Instant endAt);

    @Query("""
    select e from Event e
    where e.status = :status
      and e.startAt >= coalesce(:from, e.startAt)
      and e.startAt <= coalesce(:to,   e.startAt)
      and e.venue.id = coalesce(:venueId, e.venue.id)
""")
    Page<Event> findPublished(@Param("status") EventStatus status,
                              @Param("from")   LocalDateTime from,
                              @Param("to")     LocalDateTime to,
                              @Param("venueId") Long venueId,
                              Pageable pageable);

    Optional<Event> findByIdAndStatus(Long id, EventStatus status);

    @Query("""
           select coalesce(sum(o.total), 0)
           from Order o
           where o.event.id = :eventId
             and o.status = :status
           """)
    Optional<BigDecimal> sumOrdersByStatus(@Param("eventId") Long eventId, @Param("status") OrderStatus status);

    @Query("""
           select count(o)
           from Order o
           where o.event.id = :eventId
             and o.status = :status
           """)
    long countOrdersByStatus(@Param("eventId") Long eventId, @Param("status") OrderStatus status);

    @Query("""
           select count(oi)
           from OrderItem oi
             join oi.order o
             join o.event e
           where e.id = :eventId
             and o.status = :status
             and o.refundAt is not null
             and o.refundAt < e.startAt
           """)
    long countRefundedTickets(
            @Param("eventId") Long eventId,
            @Param("status") OrderStatus status
    );
}
