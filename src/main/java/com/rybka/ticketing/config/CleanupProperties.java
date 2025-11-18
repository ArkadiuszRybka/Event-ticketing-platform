package com.rybka.ticketing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "scheduler.cleanup")
public class CleanupProperties {
    private long fixedDelayMs = 30_000L;
    private int batchSize = 200;
    private boolean distributedLockEnabled = false;
    private long keyHolds = 1001L;
    private long keyOrders = 1002L;
}
