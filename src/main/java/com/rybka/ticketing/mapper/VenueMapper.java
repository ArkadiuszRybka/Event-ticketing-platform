package com.rybka.ticketing.mapper;

import com.rybka.ticketing.model.dto.admin.VenueReadDto;
import com.rybka.ticketing.model.entity.Venue;

public class VenueMapper {
    public static VenueReadDto toReadDto(Venue v){
        return new VenueReadDto(
          v.getId(), v.getName(), v.getRows(), v.getSeatsPerRow(), v.getTotalSeats(), v.getCreatedAt()
        );
    }
}
