package ocudni104.gateway.filter.application.exception;

public class SessionToJwtException extends RuntimeException {
  private final String reason;

  protected SessionToJwtException(String reason, String message) {
    super(message);
    this.reason = reason;
  }

  protected SessionToJwtException(String reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public String reason() {
    return reason;
  }
}
