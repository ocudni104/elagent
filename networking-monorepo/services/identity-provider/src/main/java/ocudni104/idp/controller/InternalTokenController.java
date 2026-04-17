package ocudni104.idp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class InternalTokenController {

    private final JwtEncoder jwtEncoder;

    @Value("${app.security.issuer-uri:http://localhost:8081}")
    private String issuerUri;

    public InternalTokenController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Issues a short-lived internal JWT for an authenticated session.
     * Called by the gateway to translate a browser session cookie into a JWT
     * that can be forwarded to downstream services.
     */
    @GetMapping("/internal/token")
    public ResponseEntity<Map<String, String>> issueToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUri)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300)) // 5-minute TTL; gateway fetches a fresh one per request
                .claim("roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return ResponseEntity.ok(Map.of("token", token));
    }
}
