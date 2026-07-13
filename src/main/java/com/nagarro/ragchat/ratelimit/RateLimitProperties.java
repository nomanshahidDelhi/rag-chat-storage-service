package com.nagarro.ragchat.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ragchat.rate-limit")
public record RateLimitProperties(int capacity, long refillDurationSeconds) {
}
