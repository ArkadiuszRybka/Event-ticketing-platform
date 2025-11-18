package com.rybka.ticketing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "order")
public class OrderProperties {
    private String currency = "PLN";
    private int pendingTtlSeconds;
}
