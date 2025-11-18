package com.rybka.ticketing.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.SynchronizationStrategy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitPolicies policies;
    private final RateLimitKeyResolver keyResolver;

    private final Map<String, LocalBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitPolicies policies, RateLimitKeyResolver keyResolver) {
        this.policies = policies;
        this.keyResolver = keyResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        Optional<RateLimitPolicies.Policy> opt = policies.resolve(req);
        if (opt.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }

        RateLimitPolicies.Policy policy = opt.get();
        // endpointPattern = regex polityki – tylko do klucza bucketa
        final String endpointPattern = policy.pathRegex().pattern();
        final String key = keyResolver.resolve(req);

        String bucketKey = endpointPattern + "|" + key;
        LocalBucket bucket = buckets.computeIfAbsent(bucketKey, k ->
                newBucket(policy.primary(), policy.backup(), key)
        );

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType("application/json");
            res.getWriter().write("""
                    {"error":"rate_limited","retryAfterSeconds": 1}
                    """);
        }
    }

    private LocalBucket newBucket(Bandwidth primary, Bandwidth backup, String outerKey) {
        LocalBucketBuilder builder = Bucket.builder()
                .withSynchronizationStrategy(SynchronizationStrategy.LOCK_FREE);
        if (primary != null) builder.addLimit(primary);
        if (backup != null && (primary == null || outerKey.startsWith("ip:"))) {
            builder.addLimit(backup);
        }
        return builder.build();
    }
}
