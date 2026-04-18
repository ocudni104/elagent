package ocudni104.idp.session.application;

import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.session.domain.exception.SessionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeSessionUseCaseTest {

    private static final SessionId SESSION_ID = new SessionId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Mock
    private SessionRepository sessionRepository;

    @Test
    void shouldRevokeSessionWhenRepositoryReportsSuccess() {
        RevokeSessionUseCase useCase = new RevokeSessionUseCase(sessionRepository);
        when(sessionRepository.revoke(SESSION_ID)).thenReturn(true);

        assertDoesNotThrow(() -> useCase.execute(SESSION_ID));
    }

    @Test
    void shouldThrowWhenRepositoryReportsSessionNotFound() {
        RevokeSessionUseCase useCase = new RevokeSessionUseCase(sessionRepository);
        when(sessionRepository.revoke(SESSION_ID)).thenReturn(false);

        assertThrows(SessionNotFoundException.class, () -> useCase.execute(SESSION_ID));
    }
}
