package ocudni104.idp.controller;

import jakarta.servlet.http.HttpServletRequest;
import ocudni104.idp.session.application.ValidateSessionUseCase;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class InternalTokenController {

    private final JwtEncoder jwtEncoder;
    private final ValidateSessionUseCase validateSessionUseCase;
    private final UserRepository userRepository;

    @Value("${app.security.issuer-uri:http://localhost:8081}")
    private String issuerUri;

    @Value("${auth.session.cookie-name:sid}")
    private String sessionCookieName;

    public InternalTokenController(
            JwtEncoder jwtEncoder,
            ValidateSessionUseCase validateSessionUseCase,
            UserRepository userRepository
    ) {
        this.jwtEncoder = jwtEncoder;
        this.validateSessionUseCase = validateSessionUseCase;
        this.userRepository = userRepository;
    }

    /**
     * Issues a short-lived internal JWT for an authenticated session.
     * Called by the gateway to translate a browser session cookie into a JWT
     * that can be forwarded to downstream services.
     */
    @GetMapping("/internal/token")
    public ResponseEntity<Map<String, String>> issueToken(HttpServletRequest request) {
        String rawSessionId = readSessionId(request);
        if (rawSessionId == null) {
            return ResponseEntity.status(401).build();
        }

        Session session = validateSessionUseCase.execute(new SessionId(UUID.fromString(rawSessionId)));
        var user = userRepository.findById(session.userId())
                .orElseThrow(() -> new IllegalStateException("User not found for session " + session.id().value()));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUri)
                .subject(session.userId().value().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300)) // 5-minute TTL; gateway fetches a fresh one per request
                .id(UUID.randomUUID().toString())
                .claim("sid", session.id().value().toString())
                .claim("email", user.email())
                .claim("roles", List.copyOf(session.roles()))
                .claim("scope", List.copyOf(session.scopes()))
                .claim("tenantId", session.tenantId() == null ? null : session.tenantId().value().toString())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return ResponseEntity.ok(Map.of("token", token));
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
