package com.rybka.ticketing.model.dto.admin;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventUtilizationReportDto {
    private Long eventId;
    private String title;
    private int totalSeats;
    private int free;
    private int held;
    private int locked;
    private int sold;
    private double soldPct;
}
