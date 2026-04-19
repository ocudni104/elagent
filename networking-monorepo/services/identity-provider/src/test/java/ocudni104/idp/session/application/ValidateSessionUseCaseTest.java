package ocudni104.idp.session.application;

import ocudni104.idp.session.domain.DeviceId;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;
import ocudni104.idp.session.domain.exception.SessionExpiredException;
import ocudni104.idp.session.domain.exception.SessionNotFoundException;
import ocudni104.idp.session.domain.exception.SessionRevokedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-04-19T10:00:00Z");
    private static final SessionId SESSION_ID = new SessionId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Mock
    private SessionRepository sessionRepository;

    private ValidateSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        useCase = new ValidateSessionUseCase(sessionRepository, clock);
    }

    @Test
    void shouldReturnSessionWhenSessionIsActive() {
        Session session = session(NOW.minusSeconds(60), NOW.plusSeconds(60), null);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        Session result = useCase.execute(SESSION_ID);

        assertSame(session, result);
    }

    @Test
    void shouldThrowWhenSessionIsMissing() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> useCase.execute(SESSION_ID));
    }

    @Test
    void shouldThrowWhenSessionIsRevoked() {
        Session session = session(NOW.minusSeconds(60), NOW.plusSeconds(60), NOW.minusSeconds(10));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(SessionRevokedException.class, () -> useCase.execute(SESSION_ID));
    }

    @Test
    void shouldThrowWhenSessionIsExpired() {
        Session session = session(NOW.minusSeconds(120), NOW.minusSeconds(1), null);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(SessionExpiredException.class, () -> useCase.execute(SESSION_ID));
    }

    private static Session session(Instant createdAt, Instant absoluteExpiresAt, Instant revokedAt) {
        return Session.builder()
                .id(SESSION_ID)
                .userId(new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .tenantId(new TenantId(UUID.fromString("22222222-2222-2222-2222-222222222222")))
                .deviceId(new DeviceId(UUID.fromString("33333333-3333-3333-3333-333333333333")))
                .roles(Set.of("ROLE_USER"))
                .scopes(Set.of("openid"))
                .createdAt(createdAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .revokedAt(revokedAt)
                .build();
    }
}
