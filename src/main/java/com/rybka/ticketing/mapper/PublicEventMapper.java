package com.rybka.ticketing.mapper;

import com.rybka.ticketing.model.dto.publics.EventPublicReadDto;
import com.rybka.ticketing.model.entity.Event;

public class PublicEventMapper {
    public static EventPublicReadDto toPublic(Event e) {
        return new EventPublicReadDto(
                e.getId(), e.getTitle(), e.getStartAt(), e.getEndAt(),
                e.getVenue().getId(), e.getVenue().getName(), e.getBasePrice()
        );
    }
}
