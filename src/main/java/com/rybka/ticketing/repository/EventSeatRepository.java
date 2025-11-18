package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.model.entity.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {
    boolean existsByEvent_Id(Long eventId);
    long countByEvent_Id(Long eventId);

    @Query("""
            select es.status as status, count(es) as cnt
            from EventSeat es
            where es.event.id = :eventId
            group by es.status
            """)
    List<SeatsSummaryProjection> summarizeByEvent(@Param("eventId") Long eventId);

    @Query("""
        select s from EventSeat s
        where s.event.id = :eventId
          and s.row in :rows
          and s.number in :numbers
    """)
    List<EventSeat> findPool(@Param("eventId") Long eventId,
                             @Param("rows") Collection<Integer> rows,
                             @Param("numbers") Collection<Integer> numbers);

    long countByEvent_IdAndStatus(Long eventId, SeatStatus status);

    interface SeatsSummaryProjection{
        SeatStatus getStatus();
        long getCnt();
    }


}
