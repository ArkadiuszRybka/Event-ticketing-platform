package com.rybka.ticketing.mapper;

import com.rybka.ticketing.model.dto.admin.EventReadDto;
import com.rybka.ticketing.model.entity.Event;

public class EventMapper {
    public static EventReadDto toReadDto(Event e){
        return new EventReadDto(
                e.getId(),
                e.getVenue().getId(),
                e.getVenue().getName(),
                e.getTitle(),
                e.getStartAt(),
                e.getEndAt(),
                e.getStatus().name(),
                e.getBasePrice()
        );
    }

}
