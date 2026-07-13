package com.nagarro.ragchat.session;

import com.nagarro.ragchat.common.dto.PagedResponse;
import com.nagarro.ragchat.common.exception.SessionNotFoundException;
import com.nagarro.ragchat.session.dto.ChatSessionResponse;
import com.nagarro.ragchat.session.dto.CreateSessionRequest;
import com.nagarro.ragchat.session.dto.UpdateSessionRequest;
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
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @InjectMocks
    private ChatSessionService chatSessionService;

    @Test
    void createSession_savesSession_withDefaultTitleWhenTitleBlank() {
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatSessionResponse response = chatSessionService.createSession(new CreateSessionRequest("user-1", null));

        assertThat(response.title()).isEqualTo("New Chat");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.favorite()).isFalse();
    }

    @Test
    void updateSession_renamesExistingSession_returnsUpdatedResponse() {
        UUID sessionId = UUID.randomUUID();
        ChatSession existing = new ChatSession();
        existing.setId(sessionId);
        existing.setUserId("user-1");
        existing.setTitle("Old title");

        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(existing));
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatSessionResponse response = chatSessionService.updateSession(sessionId, new UpdateSessionRequest("New title", null));

        assertThat(response.title()).isEqualTo("New title");
    }

    @Test
    void updateSession_nonExistentSession_throwsSessionNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatSessionService.updateSession(sessionId, new UpdateSessionRequest("x", null)))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void updateSession_togglesFavoriteFlag_persistsChange() {
        UUID sessionId = UUID.randomUUID();
        ChatSession existing = new ChatSession();
        existing.setId(sessionId);
        existing.setUserId("user-1");
        existing.setTitle("Chat");
        existing.setFavorite(false);

        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(existing));
        when(chatSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatSessionResponse response = chatSessionService.updateSession(sessionId, new UpdateSessionRequest(null, true));

        assertThat(response.favorite()).isTrue();
    }

    @Test
    void deleteSession_existingSession_deletesById() {
        UUID sessionId = UUID.randomUUID();
        when(chatSessionRepository.existsById(sessionId)).thenReturn(true);

        chatSessionService.deleteSession(sessionId);

        verify(chatSessionRepository).deleteById(sessionId);
    }

    @Test
    void deleteSession_nonExistentSession_throwsSessionNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        when(chatSessionRepository.existsById(sessionId)).thenReturn(false);

        assertThatThrownBy(() -> chatSessionService.deleteSession(sessionId))
                .isInstanceOf(SessionNotFoundException.class);

        verify(chatSessionRepository, never()).deleteById(any());
    }

    @Test
    void listSessions_mapsRepositoryPageToPagedResponse() {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setUserId("user-1");
        session.setTitle("Chat");

        Pageable pageable = PageRequest.of(0, 20);
        when(chatSessionRepository.findByUserId("user-1", pageable))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));

        PagedResponse<ChatSessionResponse> result = chatSessionService.listSessions("user-1", pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).userId()).isEqualTo("user-1");
    }
}
