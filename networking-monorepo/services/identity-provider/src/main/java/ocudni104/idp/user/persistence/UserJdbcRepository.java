package ocudni104.idp.user.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ocudni104.idp.user.domain.User;
import ocudni104.idp.user.domain.UserId;
import ocudni104.idp.user.domain.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository implements UserRepository {

  private final JdbcClient jdbc;

  @Override
  public Optional<User> findById(UserId id) {
    return jdbc.sql(
            """
                SELECT
                    id,
                    email,
                    provider,
                    provider_subject,
                    created_at,
                    last_login_at
                FROM users
                WHERE id = :id
                """)
        .param("id", id.value())
        .query((rs, rowNum) -> mapUser(rs))
        .optional();
  }

  @Override
  public Optional<User> findByProviderSubject(String provider, String providerSubject) {
    return jdbc.sql(
            """
                SELECT
                    id,
                    email,
                    provider,
                    provider_subject,
                    created_at,
                    last_login_at
                FROM users
                WHERE provider = :provider
                  AND provider_subject = :providerSubject
                """)
        .param("provider", provider)
        .param("providerSubject", providerSubject)
        .query((rs, rowNum) -> mapUser(rs))
        .optional();
  }

  @Override
  public void save(User user) {
    jdbc.sql(
            """
                INSERT INTO users (
                    id,
                    email,
                    provider,
                    provider_subject,
                    created_at,
                    last_login_at
                ) VALUES (
                    :id,
                    :email,
                    :provider,
                    :providerSubject,
                    :createdAt,
                    :lastLoginAt
                )
                """)
        .param("id", user.id().value())
        .param("email", user.email())
        .param("provider", user.provider())
        .param("providerSubject", user.providerSubject())
        .param("createdAt", Timestamp.from(user.createdAt()))
        .param("lastLoginAt", Timestamp.from(user.lastLoginAt()))
        .update();
  }

  @Override
  public void touchLastLogin(UserId userId, Instant lastLoginAt) {
    jdbc.sql(
            """
                UPDATE users
                SET last_login_at = :lastLoginAt
                WHERE id = :id
                """)
        .param("id", userId.value())
        .param("lastLoginAt", Timestamp.from(lastLoginAt))
        .update();
  }

  private User mapUser(ResultSet rs) throws SQLException {
    return User.builder()
        .id(new UserId(rs.getObject("id", UUID.class)))
        .email(rs.getString("email"))
        .provider(rs.getString("provider"))
        .providerSubject(rs.getString("provider_subject"))
        .createdAt(rs.getTimestamp("created_at").toInstant())
        .lastLoginAt(rs.getTimestamp("last_login_at").toInstant())
        .build();
  }
}
