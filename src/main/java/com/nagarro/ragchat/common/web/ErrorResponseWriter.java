package com.nagarro.ragchat.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nagarro.ragchat.common.dto.ErrorResponse;
import com.nagarro.ragchat.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes {@link ErrorResponse} bodies from servlet filters that run before
 * DispatcherServlet (API key auth, rate limiting), where {@code @RestControllerAdvice}
 * never gets a chance to handle the exception.
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        String requestId = MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY);
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), requestId);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
