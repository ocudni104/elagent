package ocudni104.idp.federation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.application.CreateSessionCommand;
import ocudni104.idp.session.application.CreateSessionUseCase;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.user.application.FindOrCreateUserFromFederatedLoginCommand;
import ocudni104.idp.user.application.FindOrCreateUserFromFederatedLoginUseCase;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate;
    private final FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase;
    private final CreateSessionUseCase createSessionUseCase;
    private final String sessionCookieName;

    public FederatedIdentityAuthenticationSuccessHandler(
            String defaultTargetUrl,
            FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase,
            CreateSessionUseCase createSessionUseCase,
            String sessionCookieName
    ) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(defaultTargetUrl);
        this.delegate = handler;
        this.findOrCreateUserFromFederatedLoginUseCase = findOrCreateUserFromFederatedLoginUseCase;
        this.createSessionUseCase = createSessionUseCase;
        this.sessionCookieName = sessionCookieName;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email == null) {
                email = oauthToken.getPrincipal().getName();
            }
            String subject = oauthToken.getPrincipal().getAttribute("sub");
            if (subject == null || subject.isBlank()) {
                subject = email;
            }
            var user = findOrCreateUserFromFederatedLoginUseCase.execute(
                    new FindOrCreateUserFromFederatedLoginCommand(
                            email,
                            "google",
                            subject
                    )
            );

            UsernamePasswordAuthenticationToken localAuth = UsernamePasswordAuthenticationToken.authenticated(
                    email,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER")
            );

            SecurityContextHolder.getContext().setAuthentication(localAuth);

            Session session = createSessionUseCase.execute(new CreateSessionCommand(
                    user.id(),
                    null,
                    null,
                    Set.of("ROLE_USER"),
                    oauthToken.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(a -> a.startsWith("SCOPE_"))
                            .map(a -> a.substring("SCOPE_".length()))
                            .collect(Collectors.toSet())
            ));

            ResponseCookie sidCookie = ResponseCookie.from(sessionCookieName, session.id().value().toString())
                    .httpOnly(true)
                    .path("/")
                    .sameSite("Lax")
                    .secure(false)
                    .maxAge(session.absoluteExpiresAt().getEpochSecond() - session.createdAt().getEpochSecond())
                    .build();
            response.addHeader("Set-Cookie", sidCookie.toString());
        }

        delegate.onAuthenticationSuccess(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
