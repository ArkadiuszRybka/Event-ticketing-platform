package com.rybka.ticketing.service;

import com.rybka.ticketing.mapper.EventMapper;
import com.rybka.ticketing.model.enums.EventStatus;
import com.rybka.ticketing.model.enums.SeatStatus;
import com.rybka.ticketing.model.dto.admin.EventCreateDto;
import com.rybka.ticketing.model.dto.admin.EventReadDto;
import com.rybka.ticketing.model.entity.Event;
import com.rybka.ticketing.model.entity.EventSeat;
import com.rybka.ticketing.model.entity.Venue;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import com.rybka.ticketing.repository.VenueRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;
    @Autowired
    private Clock clock;

    @Transactional
    public EventReadDto create(EventCreateDto dto){
        Venue v = venueRepository.findById(dto.venueId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));

        if(dto.endAt().isBefore(dto.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time range");
        }

        if(dto.basePrice() == null || dto.basePrice().signum() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be positive");
        }

        Event e = new Event();
        e.setVenue(v);
        e.setTitle(dto.title());
        e.setStartAt(dto.startAt());
        e.setEndAt(dto.endAt());
        e.setStatus(EventStatus.DRAFT);
        e.setBasePrice(dto.basePrice());
        e = eventRepository.save(e);
        return EventMapper.toReadDto(e);
    }

    public Page<EventReadDto> getAll(Pageable pageable, Long venueId){
        Page<Event> page = (venueId == null)
                ? eventRepository.findAll(pageable)
                : eventRepository.findByVenue_Id(venueId, pageable);
        return page.map(EventMapper::toReadDto);
    }

    public EventReadDto getById(Long id){
        Event e = eventRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return EventMapper.toReadDto(e);
    }

    @Transactional
    @CacheEvict(value = {"public_event_list", "public_event_details"}, allEntries = true, key = "#id")
    public EventReadDto publish(Long id){
        Event e = eventRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if(e.getStatus() == EventStatus.PUBLISHED){
            return EventMapper.toReadDto(e);
        }
        if(e.getStatus() == EventStatus.CANCELLED){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event already cancelled");
        }
        Instant now = Instant.now(clock);
        if(e.getStartAt().isBefore(now)){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot publish past event");
        }
        boolean overlap = eventRepository.existsOverlapVenue(
                e.getVenue().getId(), EventStatus.PUBLISHED, e.getStartAt(), e.getEndAt()
        );
        if(overlap){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event time overlap venue");
        }

        boolean seatExists = eventSeatRepository.existsByEvent_Id(e.getId());

        if(!seatExists){
            int rows = e.getVenue().getRows();
            int seatsPerRow = e.getVenue().getSeatsPerRow();


            List<EventSeat> batch = new ArrayList<>(Math.min(rows * seatsPerRow, 10000));
            for(int r = 1; r <= rows; r++){
                for (int n = 1; n <= seatsPerRow; n++){
                    EventSeat seat = new EventSeat();
                    seat.setEvent(e);
                    seat.setRow(r);
                    seat.setNumber(n);
                    seat.setStatus(SeatStatus.FREE);
                    seat.setVersion(0);

                    batch.add(seat);

                    if(batch.size() == 1000){
                        eventSeatRepository.saveAll(batch);
                        batch.clear();
                    }
                }
            }
            if(!batch.isEmpty()) eventSeatRepository.saveAll(batch);

            long cnt = eventSeatRepository.countByEvent_Id(e.getId());
            long expected = (long) rows*seatsPerRow;
            if(cnt != expected){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Event seats generation mismatch");
            }
        }
        e.setStatus(EventStatus.PUBLISHED);
        return EventMapper.toReadDto(eventRepository.save(e));
    }

    @Transactional
    @CacheEvict(value = {"public_event_list", "public_event_details"}, allEntries = true, key = "#id")
    public EventReadDto cancel(Long id, String reason){
        Event e = eventRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if(e.getStatus() == EventStatus.CANCELLED){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event already cancelled");
        }
        e.setStatus(EventStatus.CANCELLED);
        return EventMapper.toReadDto(eventRepository.save(e));
    }

}


