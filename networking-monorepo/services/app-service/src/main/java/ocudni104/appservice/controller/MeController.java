package ocudni104.appservice.controller;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    @GetMapping("/me")
    public MeResponse me(JwtAuthenticationToken authentication) {
        var token = authentication.getToken();
        return new MeResponse(
                token.getSubject(),
                token.getClaimAsString("sid"),
                token.getClaimAsString("email"),
                token.getClaimAsString("tenantId"),
                token.getClaimAsStringList("roles"),
                token.getClaimAsStringList("scope")
        );
    }

    public record MeResponse(
            String userId,
            String sessionId,
            String email,
            String tenantId,
            List<String> roles,
            List<String> scopes
    ) {}
}
