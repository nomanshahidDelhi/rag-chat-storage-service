package com.nagarro.ragchat.ratelimit;

import com.nagarro.ragchat.common.web.ErrorResponseWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory, per-client-IP token bucket rate limiter. Suitable for a single-instance
 * deployment; a distributed store (Redis) would be the production hardening step once
 * this service runs behind more than one instance.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ErrorResponseWriter errorResponseWriter;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ErrorResponseWriter errorResponseWriter) {
        this.properties = properties;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientId = resolveClientId(request);
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client {}", clientId);
            response.setHeader("Retry-After", String.valueOf(properties.refillDurationSeconds()));
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, try again later");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(properties.capacity(),
                Refill.greedy(properties.capacity(), Duration.ofSeconds(properties.refillDurationSeconds())));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
