package ocudni104.idp.session.application;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.session.domain.exception.SessionNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeSessionUseCase {

  private final SessionRepository sessionRepository;

  public void execute(SessionId sessionId) {
    boolean revoked = sessionRepository.revoke(sessionId);
    if (!revoked) {
      throw new SessionNotFoundException(sessionId);
    }
  }
}
