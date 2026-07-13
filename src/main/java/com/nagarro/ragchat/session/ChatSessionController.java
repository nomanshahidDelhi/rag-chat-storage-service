package com.nagarro.ragchat.session;

import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.session.dto.ChatSessionResponse;
import com.nagarro.ragchat.session.dto.CreateSessionRequest;
import com.nagarro.ragchat.session.dto.UpdateSessionRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat Sessions")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    public ResponseEntity<ChatSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        ChatSessionResponse response = chatSessionService.createSession(request);
        return ResponseEntity.created(URI.create("/api/v1/sessions/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ChatSessionResponse>> listSessions(
            @RequestParam @NotBlank String userId,
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatSessionService.listSessions(userId, pageable));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatSessionService.getSession(sessionId));
    }

    @PatchMapping("/{sessionId}")
    public ResponseEntity<ChatSessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionRequest request) {
        return ResponseEntity.ok(chatSessionService.updateSession(sessionId, request));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId) {
        chatSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
