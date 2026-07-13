package com.nagarro.ragchat.message.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nagarro.ragchat.message.ChatMessage;
import com.nagarro.ragchat.message.SenderType;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        SenderType sender,
        String content,
        JsonNode context,
        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getSender(),
                message.getContent(),
                message.getContext(),
                message.getCreatedAt()
        );
    }
}
