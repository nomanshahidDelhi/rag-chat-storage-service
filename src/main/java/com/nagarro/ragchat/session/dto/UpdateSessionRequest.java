package com.nagarro.ragchat.session.dto;

import jakarta.validation.constraints.Size;

public record UpdateSessionRequest(

        @Size(max = 200)
        String title,

        Boolean favorite
) {
}
