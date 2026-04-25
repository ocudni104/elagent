package ocudni104.gateway.filter.application.exception;

public final class SessionExchangeFailureReasons {

  private SessionExchangeFailureReasons() {}

  public static final String INVALID_SESSION = "invalid_session";
  public static final String IDP_5XX = "idp_5xx";
  public static final String IDP_UNAVAILABLE = "idp_unavailable";
  public static final String MISSING_TOKEN = "missing_token";
  public static final String MALFORMED_RESPONSE = "malformed_response";
  public static final String UNEXPECTED_STATUS = "unexpected_status";
}
