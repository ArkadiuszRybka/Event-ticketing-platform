package com.rybka.ticketing.service;

import com.rybka.ticketing.model.dto.admin.EventRevenueReportDto;
import com.rybka.ticketing.model.dto.admin.EventUtilizationReportDto;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.OrderItemRepository;
import com.rybka.ticketing.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Transactional(readOnly = true)
    public EventRevenueReportDto getEventRevenue(Long eventId){
        Event ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));

        BigDecimal gross = eventRepository.sumOrdersByStatus(eventId, OrderStatus.PAID).orElse(BigDecimal.ZERO);
        BigDecimal refunds = eventRepository.sumOrdersByStatus(eventId, OrderStatus.REFUNDED).orElse(BigDecimal.ZERO);

        long ordersPaid = eventRepository.countOrdersByStatus(eventId, OrderStatus.PAID);
        long ordersRefunded = eventRepository.countOrdersByStatus(eventId, OrderStatus.REFUNDED);

        long soldSeats = eventSeatRepository.countByEvent_IdAndStatus(eventId, SeatStatus.SOLD);
        long refundedSeats = eventRepository.countRefundedTickets(eventId, OrderStatus.REFUNDED);

        return EventRevenueReportDto.builder()
                .eventId(ev.getId()).title(ev.getTitle()).startAt(ev.getStartAt())
                .currency("PLN")
                .ordersPaidCount(ordersPaid)
                .ordersRefundedCount(ordersRefunded)
                .ticketsSold(soldSeats)
                .ticketsRefunded(refundedSeats)
                .grossRevenue(gross)
                .refundsTotal(refunds)
                .netRevenue(gross.subtract(refunds))
                .build();
    }

    @Transactional(readOnly = true)
    public EventUtilizationReportDto getEventUtilization(Long eventId){
        Event ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));

        Map<SeatStatus, Integer> m = new EnumMap<>(SeatStatus.class);
        for(SeatStatus s : SeatStatus.values()){
            m.put(s, (int) eventSeatRepository.countByEvent_IdAndStatus(eventId, s));
        }
        int total = ev.getVenue().getRows() * ev.getVenue().getSeatsPerRow();
        int free = m.getOrDefault(SeatStatus.FREE, 0);
        int held = m.getOrDefault(SeatStatus.HELD, 0);
        int locked = m.getOrDefault(SeatStatus.LOCKED, 0);
        int sold = m.getOrDefault(SeatStatus.SOLD, 0);
        double soldPct = total == 0 ? 0.0 : (sold * 100.0 / total);

        return EventUtilizationReportDto.builder()
                .eventId(ev.getId()).title(ev.getTitle())
                .totalSeats(total).free(free).held(held).locked(locked).sold(sold).soldPct(soldPct)
                .build();
    }
}
