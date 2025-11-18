package com.rybka.ticketing.model.dto.seat;

import java.util.List;

public record HoldRequestDto(
        List<SeatRefDto> seats
) {
}
