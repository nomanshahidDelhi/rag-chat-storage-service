package com.nagarro.ragchat.message.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nagarro.ragchat.message.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(

        @NotNull
        SenderType sender,

        @NotBlank
        @Size(max = 10000)
        String content,

        JsonNode context
) {
}
