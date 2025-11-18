package com.rybka.ticketing.controller;

import com.rybka.ticketing.config.CleanupProperties;
import com.rybka.ticketing.service.CleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    @Autowired
    private CleanupService cleanup;
    @Autowired
    private CleanupProperties props;

    @Scheduled(fixedDelayString = "${scheduler.cleanup.fixed-delay-ms}")
    public void run() {
        try {
            cleanup.expireHoldsBatch();
        } catch (Exception e) {
            log.error("[scheduler] expireHoldsBatch error ", e);
        }

        try {
            cleanup.expirePendindOrderBatch();
        } catch (Exception e) {
            log.error("[scheduler] expirePendingOrderBatch error ", e);
        }
    }
}
