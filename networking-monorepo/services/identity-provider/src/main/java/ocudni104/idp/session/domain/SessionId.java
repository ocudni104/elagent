package ocudni104.idp.session.domain;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID value) {
  public SessionId {
    Objects.requireNonNull(value);
  }

  public static SessionId newId() {
    return new SessionId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
