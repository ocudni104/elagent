package ocudni104.gateway.filter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

class SessionToJwtFilterTest {

  @Test
  void injectsBearerTokenWhenIdpReturnsToken() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"token\":\"jwt-token\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(1, chain.invocationCount());
      assertEquals(
          "Bearer jwt-token",
          chain.lastExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
      assertNull(exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsUnauthorizedWhenIdpRejectsSession() throws Exception {
    try (TestHttpServer server = TestHttpServer.responding(HttpStatus.UNAUTHORIZED.value(), "")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(0, chain.invocationCount());
      assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsServiceUnavailableWhenIdpReturnsServerError() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.INTERNAL_SERVER_ERROR.value(), "")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(0, chain.invocationCount());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsServiceUnavailableWhenIdpReturnsUnexpectedStatus() throws Exception {
    try (TestHttpServer server = TestHttpServer.responding(HttpStatus.FOUND.value(), "")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(0, chain.invocationCount());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsServiceUnavailableWhenIdpReturnsMissingToken() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"token\":\"\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(0, chain.invocationCount());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsServiceUnavailableWhenIdpReturnsMalformedBody() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"nope\":\"value\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(0, chain.invocationCount());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
  }

  @Test
  void returnsServiceUnavailableWhenIdpIsUnavailable() throws Exception {
    int port = unusedPort();
    var filter = new SessionToJwtFilter("http://127.0.0.1:" + port);
    var chain = new RecordingWebFilterChain();
    var exchange = exchange("/api/app/hello", "abc123", null);

    filter.filter(exchange, chain).block();

    assertEquals(0, chain.invocationCount());
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
  }

  @Test
  void skipsIdpCallWhenAuthorizationHeaderAlreadyExists() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"token\":\"jwt-token\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", "abc123", "Bearer existing-token");

      filter.filter(exchange, chain).block();

      assertEquals(1, chain.invocationCount());
      assertEquals(0, server.requestCount());
      assertEquals(
          "Bearer existing-token",
          chain.lastExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void skipsIdpCallWhenSessionCookieIsMissing() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"token\":\"jwt-token\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/app/hello", null, null);

      filter.filter(exchange, chain).block();

      assertEquals(1, chain.invocationCount());
      assertEquals(0, server.requestCount());
      assertNull(
          chain.lastExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void skipsIdpCallForIdpRoutes() throws Exception {
    try (TestHttpServer server =
        TestHttpServer.responding(HttpStatus.OK.value(), "{\"token\":\"jwt-token\"}")) {
      var filter = new SessionToJwtFilter(server.baseUrl());
      var chain = new RecordingWebFilterChain();
      var exchange = exchange("/api/idp/callback", "abc123", null);

      filter.filter(exchange, chain).block();

      assertEquals(1, chain.invocationCount());
      assertEquals(0, server.requestCount());
      assertNull(
          chain.lastExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
  }

  private static MockServerWebExchange exchange(
      String path, String sessionId, String authorizationHeader) {
    MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get(path);
    if (sessionId != null) {
      requestBuilder.cookie(new org.springframework.http.HttpCookie("sid", sessionId));
    }
    if (authorizationHeader != null) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }
    return MockServerWebExchange.from(requestBuilder.build());
  }

  private static int unusedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static final class RecordingWebFilterChain implements WebFilterChain {
    private int invocationCount;
    private ServerWebExchange lastExchange;

    @Override
    public reactor.core.publisher.Mono<Void> filter(ServerWebExchange exchange) {
      invocationCount++;
      lastExchange = exchange;
      return reactor.core.publisher.Mono.empty();
    }

    int invocationCount() {
      return invocationCount;
    }

    ServerWebExchange lastExchange() {
      return lastExchange;
    }
  }

  private static final class TestHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    private TestHttpServer(HttpServer server) {
      this.server = server;
    }

    static TestHttpServer responding(int statusCode, String body) throws IOException {
      HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
      TestHttpServer testServer = new TestHttpServer(server);
      server.createContext(
          "/sessions/validate",
          exchange -> {
            testServer.requestCount.incrementAndGet();
            respond(exchange, statusCode, body);
          });
      testServer.server.start();
      return testServer;
    }

    private static void respond(HttpExchange exchange, int statusCode, String body)
        throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
      exchange.sendResponseHeaders(statusCode, bytes.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(bytes);
      }
    }

    String baseUrl() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    int requestCount() {
      return requestCount.get();
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
