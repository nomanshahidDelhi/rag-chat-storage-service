package com.nagarro.ragchat.config;

import com.nagarro.ragchat.common.web.ErrorResponseWriter;
import com.nagarro.ragchat.ratelimit.RateLimitFilter;
import com.nagarro.ragchat.ratelimit.RateLimitProperties;
import com.nagarro.ragchat.security.ApiKeyAuthFilter;
import com.nagarro.ragchat.security.ApiKeyAuthenticationEntryPoint;
import com.nagarro.ragchat.security.ApiKeyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiKeyProperties apiKeyProperties,
            RateLimitProperties rateLimitProperties,
            ErrorResponseWriter errorResponseWriter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        ApiKeyAuthFilter apiKeyAuthFilter = new ApiKeyAuthFilter(apiKeyProperties);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimitProperties, errorResponseWriter);
        ApiKeyAuthenticationEntryPoint entryPoint = new ApiKeyAuthenticationEntryPoint(errorResponseWriter);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
