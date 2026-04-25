package ocudni104.idp.session.domain.exception;

import ocudni104.idp.session.domain.SessionId;

public class SessionRevokedException extends RuntimeException {
  public SessionRevokedException(SessionId sessionId) {
    super("Session revoked: " + sessionId);
  }
}
