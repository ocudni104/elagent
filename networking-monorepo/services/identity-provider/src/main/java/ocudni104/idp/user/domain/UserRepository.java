package ocudni104.idp.user.domain;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository {
  Optional<User> findById(UserId id);

  Optional<User> findByProviderSubject(String provider, String providerSubject);

  void save(User user);

  void touchLastLogin(UserId userId, Instant lastLoginAt);
}
