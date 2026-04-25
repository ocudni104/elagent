package ocudni104.idp.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieAuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String COOKIE_NAME = "oauth2_auth_request";
  private static final int COOKIE_MAX_AGE_SECONDS = 180;
  public static final String REQUEST_ATTRIBUTE_NAME =
      CookieAuthorizationRequestRepository.class.getName() + ".authorizationRequest";

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    Cookie cookie = findCookie(request, COOKIE_NAME);
    if (cookie == null || cookie.getValue().isBlank()) {
      return null;
    }

    return deserialize(cookie.getValue());
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      deleteCookie(request, response, COOKIE_NAME);
      return;
    }

    response.addHeader(
        "Set-Cookie",
        buildCookie(request, COOKIE_NAME, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS));
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
    request.setAttribute(REQUEST_ATTRIBUTE_NAME, authorizationRequest);
    deleteCookie(request, response, COOKIE_NAME);
    return authorizationRequest;
  }

  public static OAuth2AuthorizationRequest getCurrentAuthorizationRequest(
      HttpServletRequest request) {
    Object value = request.getAttribute(REQUEST_ATTRIBUTE_NAME);
    if (value instanceof OAuth2AuthorizationRequest authorizationRequest) {
      return authorizationRequest;
    }

    return null;
  }

  private Cookie findCookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) {
      return null;
    }

    for (Cookie cookie : request.getCookies()) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }

    return null;
  }

  private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
    response.addHeader("Set-Cookie", buildCookie(request, name, "", 0));
  }

  private String buildCookie(HttpServletRequest request, String name, String value, int maxAge) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(request.isSecure())
        .sameSite("Lax")
        .path("/")
        .maxAge(maxAge)
        .build()
        .toString();
  }

  private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(bytes);
      objectOutputStream.writeObject(authorizationRequest);
      objectOutputStream.flush();
      return Base64.getUrlEncoder().encodeToString(bytes.toByteArray());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to serialize OAuth2 authorization request", exception);
    }
  }

  private OAuth2AuthorizationRequest deserialize(String value) {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(value);
      ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
      return (OAuth2AuthorizationRequest) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException exception) {
      throw new IllegalStateException(
          "Failed to deserialize OAuth2 authorization request", exception);
    }
  }
}
