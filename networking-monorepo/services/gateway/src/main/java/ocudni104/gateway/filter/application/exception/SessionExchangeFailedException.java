package ocudni104.gateway.filter.application.exception;

public final class SessionExchangeFailedException extends SessionToJwtException {

  public SessionExchangeFailedException(String reason, String message) {
    super(reason, message);
  }

  public SessionExchangeFailedException(String reason, String message, Throwable cause) {
    super(reason, message, cause);
  }
}
