package com.nagarro.ragchat.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ragchat.security")
@Validated
public record ApiKeyProperties(@NotBlank String apiKey) {
}
