package ocudni104.idp.session.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.session.application.CreateSessionCommand;
import ocudni104.idp.session.application.CreateSessionUseCase;
import ocudni104.idp.session.application.RevokeSessionUseCase;
import ocudni104.idp.session.application.ValidateSessionUseCase;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;
import ocudni104.idp.user.domain.UserRepository;
import ocudni104.idp.user.domain.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final CreateSessionUseCase createSessionUseCase;
  private final JwtEncoder jwtEncoder;
  private final ValidateSessionUseCase validateSessionUseCase;
  private final RevokeSessionUseCase revokeSessionUseCase;
  private final UserRepository userRepository;

  @Value("${app.security.issuer-uri:http://localhost:8081}")
  private String issuerUri;

  @Value("${auth.session.cookie-name:sid}")
  private String sessionCookieName;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SessionResponse create(@RequestBody CreateSessionRequest request) {
    Session session =
        createSessionUseCase.execute(
            new CreateSessionCommand(
                new UserId(request.userId()),
                request.tenantId() == null ? null : new TenantId(request.tenantId()),
                request.deviceId() == null ? null : new DeviceId(request.deviceId()),
                request.roles(),
                request.scopes()));

    return SessionResponse.from(session);
  }

  @GetMapping("/validate")
  public ResponseEntity<Map<String, String>> validate(HttpServletRequest request) {
    String rawSessionId = readSessionId(request);
    if (rawSessionId == null || rawSessionId.isBlank()) {
      return ResponseEntity.status(401).build();
    }

    UUID sessionUuid;
    try {
      sessionUuid = UUID.fromString(rawSessionId);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(401).build();
    }

    Session session = validateSessionUseCase.execute(new SessionId(sessionUuid));
    var user =
        userRepository
            .findById(session.userId())
            .orElseThrow(() -> new UserNotFoundException(session.userId()));
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuerUri)
            .subject(session.userId().value().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .id(UUID.randomUUID().toString())
            .claim("sid", session.id().value().toString())
            .claim("email", user.email())
            .claim("roles", List.copyOf(session.roles()))
            .claim("scope", List.copyOf(session.scopes()))
            .claim(
                "tenantId",
                session.tenantId() == null ? null : session.tenantId().value().toString())
            .build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(Map.of("token", token));
  }

  @PostMapping("/{id}/revoke")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable UUID id) {
    revokeSessionUseCase.execute(new SessionId(id));
  }

  private String readSessionId(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (var cookie : request.getCookies()) {
      if (sessionCookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
