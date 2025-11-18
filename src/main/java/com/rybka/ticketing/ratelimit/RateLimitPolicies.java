package com.rybka.ticketing.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class RateLimitPolicies {

    public record Policy(
            HttpMethod method,
            Pattern pathRegex,
            Bandwidth primary,   // np. 10 / min na USER
            Bandwidth backup     // np. 30 / 5 min na IP (opcjonalnie)
    ) { }

    private final List<Policy> policies = List.of(
            // AUTH
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/auth/login$"),
                    Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build(),
                    Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofHours(1)).build()
            ),
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/auth/register$"),
                    null,
                    Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofHours(1)).build()
            ),

            // HOLD
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/events/\\d+/hold$"),
                    Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build(),
                    Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofMinutes(5)).build()
            ),

            // ORDERS
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/orders$"),
                    Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build(),
                    null
            ),

            // PAYMENTS INIT
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/payments$"),
                    Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build(),
                    null
            ),

            // WEBHOOK – zwykle nie limitujemy, ale zostawiamy “bezpiecznik” na IP
            new Policy(
                    HttpMethod.POST,
                    Pattern.compile("^/api/payments/webhook$"),
                    null,
                    Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofMinutes(1)).build()
            )
    );

    public Optional<Policy> resolve(String httpMethod, String path) {
        HttpMethod m = null;
        for (HttpMethod x : HttpMethod.values()) {
            if (x.name().equalsIgnoreCase(httpMethod)) { m = x; break; }
        }
        if (m == null) return Optional.empty();

        for (Policy p : policies) {
            if (p.method() == m && p.pathRegex().matcher(path).matches()) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<Policy> resolve(HttpServletRequest req) {
        return resolve(req.getMethod(), req.getRequestURI());
    }
}