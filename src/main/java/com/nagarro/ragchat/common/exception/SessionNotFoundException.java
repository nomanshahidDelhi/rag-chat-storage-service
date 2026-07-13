package com.nagarro.ragchat.common.exception;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Chat session not found: " + sessionId);
    }
}
