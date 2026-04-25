package ocudni104.idp.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import ocudni104.idp.user.domain.User;
import ocudni104.idp.user.domain.UserId;
import ocudni104.idp.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindOrCreateUserFromFederatedLoginUseCaseTest {

  private static final Instant NOW = Instant.parse("2026-04-19T12:00:00Z");

  @Mock private UserRepository userRepository;

  private FindOrCreateUserFromFederatedLoginUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new FindOrCreateUserFromFederatedLoginUseCase(
            userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldCreateAndPersistUserWhenMissing() {
    FindOrCreateUserFromFederatedLoginCommand command = command();
    when(userRepository.findByProviderSubject(command.provider(), command.providerSubject()))
        .thenReturn(Optional.empty());

    User user = useCase.execute(command);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(user, captor.getValue());
    assertEquals(command.email(), user.email());
    assertEquals(command.provider(), user.provider());
    assertEquals(command.providerSubject(), user.providerSubject());
    assertEquals(NOW, user.createdAt());
    assertEquals(NOW, user.lastLoginAt());
    assertNotNull(user.id());
  }

  @Test
  void shouldReturnExistingUserAndTouchLastLogin() {
    FindOrCreateUserFromFederatedLoginCommand command = command();
    User existing =
        User.builder()
            .id(new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")))
            .email(command.email())
            .provider(command.provider())
            .providerSubject(command.providerSubject())
            .createdAt(NOW.minusSeconds(3600))
            .lastLoginAt(NOW.minusSeconds(1800))
            .build();

    when(userRepository.findByProviderSubject(command.provider(), command.providerSubject()))
        .thenReturn(Optional.of(existing));

    User result = useCase.execute(command);

    verify(userRepository).touchLastLogin(existing.id(), NOW);
    assertEquals(existing.id(), result.id());
    assertEquals(existing.createdAt(), result.createdAt());
    assertEquals(NOW, result.lastLoginAt());
  }

  private static FindOrCreateUserFromFederatedLoginCommand command() {
    return new FindOrCreateUserFromFederatedLoginCommand(
        "user@example.com", "google", "google-subject-123");
  }
}
