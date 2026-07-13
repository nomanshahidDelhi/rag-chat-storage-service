package com.nagarro.ragchat.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.common.exception.SessionNotFoundException;
import com.nagarro.ragchat.message.dto.ChatMessageResponse;
import com.nagarro.ragchat.message.dto.CreateMessageRequest;
import com.nagarro.ragchat.session.ChatSession;
import com.nagarro.ragchat.session.ChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void addMessage_existingSession_savesMessageLinkedToSession() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);

        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = chatMessageService.addMessage(sessionId,
                new CreateMessageRequest(SenderType.USER, "Hello", null));

        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.content()).isEqualTo("Hello");
        assertThat(response.sender()).isEqualTo(SenderType.USER);
    }

    @Test
    void addMessage_nonExistentSession_throwsSessionNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.addMessage(sessionId,
                new CreateMessageRequest(SenderType.USER, "Hello", null)))
                .isInstanceOf(SessionNotFoundException.class);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void addMessage_withNullContext_savesSuccessfully() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);

        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode context = null;
        ChatMessageResponse response = chatMessageService.addMessage(sessionId,
                new CreateMessageRequest(SenderType.ASSISTANT, "Here is the answer", context));

        assertThat(response.context()).isNull();
    }

    @Test
    void getMessages_existingSession_returnsPagedMessages() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setSession(session);
        message.setSender(SenderType.USER);
        message.setContent("Hi");

        Pageable pageable = PageRequest.of(0, 20);
        when(chatSessionRepository.existsById(sessionId)).thenReturn(true);
        when(chatMessageRepository.findBySessionId(sessionId, pageable))
                .thenReturn(new PageImpl<>(List.of(message), pageable, 1));

        PagedResponse<ChatMessageResponse> result = chatMessageService.getMessages(sessionId, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).sessionId()).isEqualTo(sessionId);
    }

    @Test
    void getMessages_nonExistentSession_throwsSessionNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(chatSessionRepository.existsById(sessionId)).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.getMessages(sessionId, pageable))
                .isInstanceOf(SessionNotFoundException.class);
    }
}
