package com.rybka.ticketing.repository;

import com.rybka.ticketing.model.dto.admin.AdminOrderListRow;
import com.rybka.ticketing.model.dto.userOrder.MyOrderListRow;
import com.rybka.ticketing.model.entity.Order;
import com.rybka.ticketing.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findWithItemsById(Long id);

    Page<Order> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus status, Instant before, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);


    @Query("""
            select
              o.id as orderId,
              o.status as status,
              o.createdAt as createdAt,
              e.id as eventId,
              e.title as eventTitle,
              e.startAt as eventStartAt,
              v.name as venueName,
              count(oi.id) as itemsCount,
              o.total as total,
              o.currency as currency
            from Order o
              join o.event e
              join e.venue v
              left join OrderItem oi on oi.order.id = o.id
            where o.userId = :userId
                and o.createdAt >= coalesce(:from, o.createdAt)
                and o.createdAt <= coalesce(:to,   o.createdAt)
                and e.id        =  coalesce(:eventId, e.id)
                and ( :statusesEmpty = true or o.status in :statuses )
            group by o.id, o.status, o.createdAt, e.id, e.title, e.startAt, v.name, o.total, o.currency
            """)
    Page<MyOrderListRow> searchMyOrders(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("eventId") Long eventId,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable
    );

    @Query(
            value = """
    select
      o.id                               as orderId,
      cast(o.status as string)           as status,
      o.createdAt                        as createdAt,
      u.email                            as userEmail,
      e.id                               as eventId,
      e.title                            as eventTitle,
      e.startAt                          as eventStartAt,
      v.name                             as venueName,
      count(oi.id)                       as itemsCount,
      o.total                            as total,
      o.currency                         as currency
    from Order o
      join User u on u.id = o.userId
      join o.event e
      join e.venue v
      left join OrderItem oi on oi.order.id = o.id
    where
      o.createdAt >= coalesce(:from, o.createdAt)
      and o.createdAt <= coalesce(:to,   o.createdAt)
      and e.id        =  coalesce(:eventId, e.id)
      and lower(u.email) like coalesce(lower(concat('%', :userEmail, '%')), lower(u.email))
      and ( :statusesEmpty = true or o.status in :statuses )
    group by o.id, o.status, o.createdAt, u.email, e.id, e.title, e.startAt, v.name, o.total, o.currency
  """,
            countQuery = """
    select count(distinct o.id)
    from Order o
      join User u on u.id = o.userId
      join o.event e
      join e.venue v
      left join OrderItem oi on oi.order.id = o.id
    where
      o.createdAt >= coalesce(:from, o.createdAt)
      and o.createdAt <= coalesce(:to,   o.createdAt)
      and e.id        =  coalesce(:eventId, e.id)
      and lower(u.email) like coalesce(lower(concat('%', :userEmail, '%')), lower(u.email))
      and ( :statusesEmpty = true or o.status in :statuses )
  """
    )
    Page<AdminOrderListRow> adminSearchOrders(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("eventId") Long eventId,
            @Param("userEmail") String userEmail,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable
    );
}
