package com.rybka.ticketing.service;

import com.rybka.ticketing.mapper.PublicEventMapper;
import com.rybka.ticketing.model.enums.EventStatus;
import com.rybka.ticketing.model.dto.publics.EventDetailsReadDto;
import com.rybka.ticketing.model.dto.publics.EventPublicReadDto;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.EventSeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class PublicEventService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;

    public Page<EventPublicReadDto> listPublished(LocalDateTime from, LocalDateTime to, Long venueId, Pageable pageable){
        if(from != null && to != null && from.isAfter(to)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        var page = eventRepository.findPublished(EventStatus.PUBLISHED, from, to, venueId, pageable);
        return page.map(PublicEventMapper::toPublic);
    }

    @Cacheable(
            value = "public_event_details",
            key = "#eventId"
    )
    public EventDetailsReadDto getDetails(Long eventId){
        var e = eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found or not published"));

        long free=0, held=0, locked=0, sold=0;
        for(var row : eventSeatRepository.summarizeByEvent(e.getId())) {
            switch (row.getStatus()){
                case FREE   -> free     = row.getCnt();
                case HELD   -> held     = row.getCnt();
                case LOCKED -> locked   = row.getCnt();
                case SOLD   -> sold     = row.getCnt();
            }
        }
        int rows = e.getVenue().getRows();
        int spr = e.getVenue().getSeatsPerRow();
        int total = rows * spr;

        var sum = new EventDetailsReadDto.SeatsSummary(free, held, locked, sold);
        return new EventDetailsReadDto(
                e.getId(), e.getTitle(), e.getStartAt(), e.getEndAt(),
                e.getVenue().getId(), e.getVenue().getName(), rows, spr, total,
                e.getBasePrice(), sum
        );
    }

    public String buildETagSource(EventDetailsReadDto d) {
        return d.id()+"|"+d.startAt()+"|"+d.endAt()+"|"+
                d.seatsSummary().free()+"|"+d.seatsSummary().held()+"|"+
                d.seatsSummary().locked()+"|"+d.seatsSummary().sold();
    }
}
