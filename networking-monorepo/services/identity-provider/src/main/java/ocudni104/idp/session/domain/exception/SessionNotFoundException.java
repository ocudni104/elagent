package ocudni104.idp.session.domain.exception;

import ocudni104.idp.session.domain.SessionId;

public class SessionNotFoundException extends RuntimeException {
  public SessionNotFoundException(SessionId sessionId) {
    super("Session not found: " + sessionId);
  }
}
