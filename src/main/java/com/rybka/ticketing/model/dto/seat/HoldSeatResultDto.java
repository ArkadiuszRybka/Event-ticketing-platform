package com.rybka.ticketing.model.dto.seat;

public record HoldSeatResultDto(
        int row,
        int number,
        HoldSeatState status
) {
}
