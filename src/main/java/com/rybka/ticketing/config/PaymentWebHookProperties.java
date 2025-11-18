package com.rybka.ticketing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "payment.webhook")
public class PaymentWebHookProperties {
    private String secret;
    private long tsToleranceSec=300;
}
