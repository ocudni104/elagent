package ocudni104.idp.session.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateSessionUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-04-19T10:00:00Z");
  private static final Duration SESSION_TTL = Duration.ofHours(12);

  @Mock private SessionRepository sessionRepository;

  private CreateSessionUseCase useCase;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    useCase = new CreateSessionUseCase(sessionRepository, clock);
    ReflectionTestUtils.setField(useCase, "sessionTtl", SESSION_TTL);
  }

  @Test
  void shouldSetCreatedAtFromClock() {
    Session session = useCase.execute(command());

    assertEquals(NOW, session.createdAt());
  }

  @Test
  void shouldComputeAbsoluteExpiryFromConfiguredTtl() {
    Session session = useCase.execute(command());

    assertEquals(NOW.plus(SESSION_TTL), session.absoluteExpiresAt());
  }

  @Test
  void shouldPersistCreatedSession() {
    Session session = useCase.execute(command());

    ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
    verify(sessionRepository).save(sessionCaptor.capture());
    assertEquals(session, sessionCaptor.getValue());
  }

  @Test
  void shouldReturnCreatedSession() {
    Session session = useCase.execute(command());

    assertNotNull(session);
    assertNotNull(session.id());
    assertNotNull(session.id().value());
  }

  @Test
  void shouldCopyCommandDataIntoSession() {
    CreateSessionCommand command = command();

    Session session = useCase.execute(command);

    assertEquals(command.userId(), session.userId());
    assertEquals(command.tenantId(), session.tenantId());
    assertEquals(command.deviceId(), session.deviceId());
    assertEquals(command.roles(), session.roles());
    assertEquals(command.scopes(), session.scopes());
  }

  private static CreateSessionCommand command() {
    return new CreateSessionCommand(
        new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
        new TenantId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
        new DeviceId(UUID.fromString("33333333-3333-3333-3333-333333333333")),
        Set.of("ROLE_USER", "ROLE_ADMIN"),
        Set.of("openid", "profile"));
  }
}
