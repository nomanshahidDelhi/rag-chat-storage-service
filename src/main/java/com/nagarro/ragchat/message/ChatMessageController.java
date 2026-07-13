package com.nagarro.ragchat.message;

import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.message.dto.ChatMessageResponse;
import com.nagarro.ragchat.message.dto.CreateMessageRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat Messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> addMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreateMessageRequest request) {
        ChatMessageResponse response = chatMessageService.addMessage(sessionId, request);
        return ResponseEntity.created(URI.create("/api/v1/sessions/" + sessionId + "/messages/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ChatMessageResponse>> getMessages(
            @PathVariable UUID sessionId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(chatMessageService.getMessages(sessionId, pageable));
    }
}
