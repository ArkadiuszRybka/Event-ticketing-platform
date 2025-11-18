package com.rybka.ticketing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hold")
@Data
public class HoldProperties {
    private int ttlSeconds = 120;
    private int maxSeats = 10;
}
