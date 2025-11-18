package com.rybka.ticketing.model.dto.admin;

public record VenueUpdateDto(
        String name,
        Integer row,
        Integer seatsPerRow
) {
}
