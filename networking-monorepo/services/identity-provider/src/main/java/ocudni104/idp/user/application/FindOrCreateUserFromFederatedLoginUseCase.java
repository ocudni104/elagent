package ocudni104.idp.user.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ocudni104.idp.user.domain.User;
import ocudni104.idp.user.domain.UserId;
import ocudni104.idp.user.domain.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindOrCreateUserFromFederatedLoginUseCase {

  private final UserRepository userRepository;
  private final Clock clock;

  public User execute(FindOrCreateUserFromFederatedLoginCommand cmd) {
    Instant now = clock.instant();

    return userRepository
        .findByProviderSubject(cmd.provider(), cmd.providerSubject())
        .map(
            existing -> {
              userRepository.touchLastLogin(existing.id(), now);
              return existing.toBuilder().lastLoginAt(now).build();
            })
        .orElseGet(
            () -> {
              User user =
                  User.builder()
                      .id(new UserId(UUID.randomUUID()))
                      .email(cmd.email())
                      .provider(cmd.provider())
                      .providerSubject(cmd.providerSubject())
                      .createdAt(now)
                      .lastLoginAt(now)
                      .build();
              userRepository.save(user);
              return user;
            });
  }
}
