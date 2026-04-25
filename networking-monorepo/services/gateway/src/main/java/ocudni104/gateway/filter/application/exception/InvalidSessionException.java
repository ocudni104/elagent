package ocudni104.gateway.filter.application.exception;

import static ocudni104.gateway.filter.application.exception.SessionExchangeFailureReasons.INVALID_SESSION;

public final class InvalidSessionException extends SessionToJwtException {

  public InvalidSessionException(String message) {
    super(INVALID_SESSION, message);
  }

  public InvalidSessionException(String message, Throwable cause) {
    super(INVALID_SESSION, message, cause);
  }
}
