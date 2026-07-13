package com.nagarro.ragchat.common.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId
) {

    public static ErrorResponse of(int status, String error, String message, String path, String requestId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, requestId);
    }
}
