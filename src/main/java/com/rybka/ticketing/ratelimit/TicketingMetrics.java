package com.rybka.ticketing.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class TicketingMetrics {

    private final MeterRegistry registry;

    public TicketingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void holdRequest(String outcome) {
        Counter.builder("ticketing.hold.requests")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void orderCreated(String status) {
        Counter.builder("ticketing.order.created")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void paymentWebhook(String status, boolean applied) {
        Counter.builder("ticketing.payment.webhook")
                .tag("status", status)
                .tag("applied", String.valueOf(applied))
                .register(registry)
                .increment();
    }

    public void schedulerHoldsExpired(long count) {
        Counter.builder("ticketing.scheduler.holds.expired")
                .register(registry)
                .increment(count);
    }

    public void schedulerOrdersExpired(long count) {
        Counter.builder("ticketing.scheduler.orders.expired")
                .register(registry)
                .increment(count);
    }


    public Sample startHoldTimer() {
        Timer.Sample s = Timer.start(registry);
        return new Sample(s, "ticketing.hold.latency");
    }

    public Sample startOrderTimer() {
        Timer.Sample s = Timer.start(registry);
        return new Sample(s, "ticketing.order.latency");
    }

    public final class Sample {
        private final Timer.Sample sample;
        private final String metricName;

        private Sample(Timer.Sample sample, String metricName) {
            this.sample = sample;
            this.metricName = metricName;
        }

        public void stop(String outcome) {
            Timer timer = Timer.builder(metricName)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .tag("outcome", outcome)
                    .register(registry);
            sample.stop(timer);
        }
    }
}