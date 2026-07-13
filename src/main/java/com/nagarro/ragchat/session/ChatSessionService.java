package com.nagarro.ragchat.session;

import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.common.exception.SessionNotFoundException;
import com.nagarro.ragchat.session.dto.ChatSessionResponse;
import com.nagarro.ragchat.session.dto.CreateSessionRequest;
import com.nagarro.ragchat.session.dto.UpdateSessionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private static final String DEFAULT_TITLE = "New Chat";

    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public ChatSessionResponse createSession(CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(request.userId());
        session.setTitle(request.title() == null || request.title().isBlank() ? DEFAULT_TITLE : request.title());
        session.setFavorite(false);

        ChatSession saved = chatSessionRepository.save(session);
        log.info("Created chat session {} for user {}", saved.getId(), saved.getUserId());
        return ChatSessionResponse.from(saved);
    }

    public PagedResponse<ChatSessionResponse> listSessions(String userId, Pageable pageable) {
        return PagedResponse.from(chatSessionRepository.findByUserId(userId, pageable), ChatSessionResponse::from);
    }

    public ChatSessionResponse getSession(UUID sessionId) {
        return ChatSessionResponse.from(getOrThrow(sessionId));
    }

    @Transactional
    public ChatSessionResponse updateSession(UUID sessionId, UpdateSessionRequest request) {
        ChatSession session = getOrThrow(sessionId);

        if (request.title() != null) {
            session.setTitle(request.title());
        }
        if (request.favorite() != null) {
            session.setFavorite(request.favorite());
        }

        ChatSession saved = chatSessionRepository.save(session);
        log.info("Updated chat session {}", saved.getId());
        return ChatSessionResponse.from(saved);
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        chatSessionRepository.deleteById(sessionId);
        log.info("Deleted chat session {}", sessionId);
    }

    private ChatSession getOrThrow(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }
}
