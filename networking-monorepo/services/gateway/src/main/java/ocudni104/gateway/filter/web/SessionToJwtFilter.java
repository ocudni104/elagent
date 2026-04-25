package ocudni104.gateway.filter.web;

import static ocudni104.gateway.filter.application.exception.SessionExchangeFailureReasons.*;

import lombok.extern.slf4j.Slf4j;
import ocudni104.gateway.filter.application.exception.InvalidSessionException;
import ocudni104.gateway.filter.application.exception.SessionExchangeFailedException;
import ocudni104.gateway.filter.application.exception.SessionToJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Runs before Spring Security (-100). When a browser request carries a sid cookie but no
 * Authorization header, this filter calls the IDP's /sessions/validate endpoint to exchange the
 * session for a short-lived JWT, then injects it as "Authorization: Bearer …" so the downstream JWT
 * resource-server validation works transparently.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionToJwtFilter implements WebFilter {

  private final WebClient idpClient;
  private static final String SESSION_COOKIE_NAME = "sid";

  public SessionToJwtFilter(@Value("${app.idp.internal-uri:http://localhost:8081}") String idpUri) {
    this.idpClient = WebClient.builder().baseUrl(idpUri).build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Skip IDP routes — they handle the OAuth2 login flow directly and must not have
    // their session touched here; doing so causes session fixation migration on the IDP
    // which clears the stored OAuth2 state and breaks the Google callback.
    if (exchange.getRequest().getPath().value().startsWith("/api/idp/")) {
      return chain.filter(exchange);
    }

    // Skip if a bearer token is already present
    if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
      return chain.filter(exchange);
    }

    var sessionCookie = exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME);
    if (sessionCookie == null) {
      return chain.filter(exchange);
    }

    return exchangeSessionForToken(sessionCookie.getValue())
        .flatMap(
            token -> {
              var mutated =
                  exchange
                      .getRequest()
                      .mutate()
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                      .build();
              return chain.filter(exchange.mutate().request(mutated).build());
            })
        .onErrorResume(
            SessionToJwtException.class,
            ex -> {
              log.warn(
                  "session_to_jwt_exchange_failed reason={} path={} method={} message={}",
                  ex.reason(),
                  exchange.getRequest().getPath().value(),
                  exchange.getRequest().getMethod(),
                  ex.getMessage(),
                  ex);

              exchange.getResponse().setStatusCode(statusFor(ex));
              return exchange.getResponse().setComplete();
            });
  }

  private Mono<String> exchangeSessionForToken(String sessionId) {
    return idpClient
        .get()
        .uri("/sessions/validate")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchangeToMono(
            response ->
                switch (classify(response.statusCode())) {
                  case SUCCESS -> readToken(response);
                  case CLIENT_ERROR -> invalidSession(response);
                  case SERVER_ERROR -> idpServerFailure(response);
                  case UNEXPECTED -> unexpectedStatus(response);
                })
        .onErrorMap(
            WebClientRequestException.class,
            ex ->
                new SessionExchangeFailedException(
                    IDP_UNAVAILABLE, "Could not reach IDP session validation endpoint", ex));
  }

  private SessionValidationStatus classify(HttpStatusCode status) {
    if (status.is2xxSuccessful()) {
      return SessionValidationStatus.SUCCESS;
    }

    if (status.is4xxClientError()) {
      return SessionValidationStatus.CLIENT_ERROR;
    }

    if (status.is5xxServerError()) {
      return SessionValidationStatus.SERVER_ERROR;
    }

    return SessionValidationStatus.UNEXPECTED;
  }

  private enum SessionValidationStatus {
    SUCCESS,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNEXPECTED
  }

  private HttpStatus statusFor(SessionToJwtException ex) {
    return switch (ex.reason()) {
      case INVALID_SESSION -> HttpStatus.UNAUTHORIZED;
      case IDP_5XX, IDP_UNAVAILABLE, MISSING_TOKEN, MALFORMED_RESPONSE, UNEXPECTED_STATUS ->
          HttpStatus.SERVICE_UNAVAILABLE;
      default -> HttpStatus.SERVICE_UNAVAILABLE;
    };
  }

  private Mono<String> readToken(ClientResponse response) {
    return response
        .bodyToMono(ValidateSessionResponse.class)
        .switchIfEmpty(
            Mono.error(
                new SessionExchangeFailedException(
                    MALFORMED_RESPONSE, "IDP returned an empty session validation response")))
        .flatMap(
            body -> {
              if (body.token() == null || body.token().isBlank()) {
                return Mono.error(
                    new SessionExchangeFailedException(
                        MISSING_TOKEN, "IDP returned 2xx but no token"));
              }

              return Mono.just(body.token());
            })
        .onErrorMap(
            ex -> {
              if (ex instanceof SessionToJwtException sessionToJwtException) {
                return sessionToJwtException;
              }

              return new SessionExchangeFailedException(
                  MALFORMED_RESPONSE, "Could not parse IDP validation response", ex);
            });
  }

  private Mono<String> invalidSession(ClientResponse response) {
    return response
        .releaseBody()
        .then(
            Mono.error(
                new InvalidSessionException(
                    "IDP rejected session with status " + response.statusCode().value())));
  }

  private Mono<String> idpServerFailure(ClientResponse response) {
    return response
        .releaseBody()
        .then(
            Mono.error(
                new SessionExchangeFailedException(
                    IDP_5XX, "IDP returned status " + response.statusCode().value())));
  }

  private Mono<String> unexpectedStatus(ClientResponse response) {
    return response
        .releaseBody()
        .then(
            Mono.error(
                new SessionExchangeFailedException(
                    UNEXPECTED_STATUS, "Unexpected IDP status " + response.statusCode().value())));
  }
}
