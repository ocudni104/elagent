package ocudni104.idp.session.domain;

import java.util.Optional;

public interface SessionRepository {
  Optional<Session> findById(SessionId id);

  void save(Session session);

  boolean revoke(SessionId id);
}
