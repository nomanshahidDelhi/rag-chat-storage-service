package com.nagarro.ragchat.message;

import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.common.exception.SessionNotFoundException;
import com.nagarro.ragchat.message.dto.ChatMessageResponse;
import com.nagarro.ragchat.message.dto.CreateMessageRequest;
import com.nagarro.ragchat.session.ChatSession;
import com.nagarro.ragchat.session.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public ChatMessageResponse addMessage(UUID sessionId, CreateMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSender(request.sender());
        message.setContent(request.content());
        message.setContext(request.context());

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("Added {} message {} to session {}", saved.getSender(), saved.getId(), sessionId);
        return ChatMessageResponse.from(saved);
    }

    public PagedResponse<ChatMessageResponse> getMessages(UUID sessionId, Pageable pageable) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        return PagedResponse.from(chatMessageRepository.findBySessionId(sessionId, pageable), ChatMessageResponse::from);
    }
}
