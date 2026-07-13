package com.nagarro.ragchat.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(

        @NotBlank
        @Size(max = 100)
        String userId,

        @Size(max = 200)
        String title
) {
}
