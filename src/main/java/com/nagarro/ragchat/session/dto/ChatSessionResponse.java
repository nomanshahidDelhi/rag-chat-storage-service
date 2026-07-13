package com.nagarro.ragchat.session.dto;

import com.nagarro.ragchat.session.ChatSession;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        String userId,
        String title,
        boolean favorite,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getUserId(),
                session.getTitle(),
                session.isFavorite(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
