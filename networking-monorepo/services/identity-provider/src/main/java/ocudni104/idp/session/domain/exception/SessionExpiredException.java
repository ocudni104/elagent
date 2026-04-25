package ocudni104.idp.session.domain.exception;

import ocudni104.idp.session.domain.SessionId;

public class SessionExpiredException extends RuntimeException {
  public SessionExpiredException(SessionId sessionId) {
    super("Session expired: " + sessionId);
  }
}
