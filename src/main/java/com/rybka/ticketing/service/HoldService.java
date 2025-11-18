package com.rybka.ticketing.service;

import com.rybka.ticketing.config.HoldProperties;
import com.rybka.ticketing.model.enums.EventStatus;
import com.rybka.ticketing.model.enums.HoldStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.model.dto.seat.HoldResponseDto;
import com.rybka.ticketing.model.dto.seat.HoldSeatResultDto;
import com.rybka.ticketing.model.dto.seat.HoldSeatState;
import com.rybka.ticketing.model.dto.seat.SeatRefDto;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.entity.EventSeat;
import com.rybka.ticketing.model.entity.ReservationHold;
import com.rybka.ticketing.model.entity.ReservationHoldItem;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.ReservationHoldRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HoldService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;
    @Autowired
    private ReservationHoldRepository reservationHoldRepository;
    @Autowired
    private HoldProperties props;

    private static final int MAX_RETRIES = 3;

    @Transactional(readOnly = true)
    public Event requirePublishedEvent(Long eventId){
        return eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found or not published"));
    }

    public HoldResponseDto hold(Long eventId, Long userId, String idempotencyKey, List<SeatRefDto> seats){
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing idempotency key");
        }
        if (seats == null || seats.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: empty seats");

        if (seats.size() > props.getMaxSeats())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: too many seats");

        Set<String> uniq = new HashSet<>();
        for(SeatRefDto s : seats){
            if(s.row() < 1 || s.number() < 1)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: row/number >= 1");
            String key = s.row() + ":" + s.number();
            if(!uniq.add(key))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: duplicate seat " + key);
        }

        Event event = requirePublishedEvent(eventId);
        int maxRow = event.getVenue().getRows();
        int maxInRow = event.getVenue().getSeatsPerRow();
        for(SeatRefDto s : seats){
            if(s.row() > maxRow || s.number() > maxInRow){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: out_of_bounds " + s.row()+"/"+s.number());
            }
        }

        Optional<ReservationHold> existing = reservationHoldRepository
                .findByUserIdAndEvent_IdAndIdempotencyKey(userId, eventId, idempotencyKey);

        if(existing.isPresent()){
            Set<String> requested = seats.stream().map(seat -> seat.row()+":"+seat.number()).collect(Collectors.toSet());
            Set<String> already = existing.get().getItems().stream().map(i -> i.getRow()+":"+i.getNumber()).collect(Collectors.toSet());
            if(!requested.equals(already))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "idempotency payload mismatch");
            return toResponse(existing.get(), HoldSeatState.HELD, props.getTtlSeconds());
        }

        int attempts = 0;
        while (true) {
            try {
                return doHoldTx(event, userId, idempotencyKey, seats);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                if (++attempts >= MAX_RETRIES) throw ex;
                try { Thread.sleep(20L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Transactional
    protected HoldResponseDto doHoldTx(Event event, Long userId, String idempotencyKey, List<SeatRefDto> seats){
        Long eventId = event.getId();

        List<Integer> rows = seats.stream().map(SeatRefDto::row).distinct().toList();
        List<Integer> numbers = seats.stream().map(SeatRefDto::number).distinct().toList();
        List<EventSeat> pool = eventSeatRepository.findPool(eventId, rows, numbers);

        Map<String, EventSeat> byKey = pool.stream()
                .collect(Collectors.toMap(s -> s.getRow()+":"+s.getNumber(), Function.identity(), (a,b) -> a));

        List<String> missing = new ArrayList<>();
        for(SeatRefDto req : seats){
            if(!byKey.containsKey(req.row()+":"+ req.number())) {
                missing.add(req.row()+":"+ req.number());
            }
        }

        if(!missing.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validation_failed: seats_not_found " + missing);
        }

        List<HoldSeatResultDto> result = new ArrayList<>(seats.size());
        boolean anyConflict = false;
        for(SeatRefDto req : seats){
            EventSeat seat = byKey.get(req.row()+":"+ req.number());
            if(seat.getStatus() != SeatStatus.FREE){
                anyConflict = true;
                result.add(new HoldSeatResultDto(req.row(), req.number(), HoldSeatState.CONFLICT));
            } else {
                result.add(new HoldSeatResultDto(req.row(), req.number(), HoldSeatState.HELD) );
            }
        }
        if (anyConflict){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seats not free");
        }

        for(SeatRefDto req : seats){
            EventSeat seat = byKey.get(req.row()+":"+ req.number());
            seat.setStatus(SeatStatus.HELD);
        }

        eventSeatRepository.saveAllAndFlush(byKey.values());

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getTtlSeconds());
        final ReservationHold hold = new ReservationHold();
        hold.setUserId(userId);
        hold.setEvent(event);
        hold.setCreatedAt(now);
        hold.setExpiresAt(exp);
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setIdempotencyKey(idempotencyKey);

        List<ReservationHoldItem> items = seats.stream()
                .map(s -> ReservationHoldItem.builder()
                        .hold(hold).row(s.row()).number(s.number()).build())
                .collect(Collectors.toList());
        hold.setItems(new ArrayList<>(items));

        reservationHoldRepository.save(hold);

        return new HoldResponseDto(
                hold.getId(),
                event.getId(),
                hold.getExpiresAt(),
                seats.stream().map(s -> new HoldSeatResultDto(s.row(), s.number(), HoldSeatState.HELD)).toList(),
                props.getTtlSeconds()
        );
    }


    private HoldResponseDto toResponse(ReservationHold hold, HoldSeatState state, Integer ttl) {
        List<HoldSeatResultDto> seatResults = hold.getItems().stream()
                .map(i -> new HoldSeatResultDto(i.getRow(), i.getNumber(), state))
                .toList();
        return new HoldResponseDto(hold.getId(), hold.getEvent().getId(), hold.getExpiresAt(), seatResults, ttl);
    }
}
