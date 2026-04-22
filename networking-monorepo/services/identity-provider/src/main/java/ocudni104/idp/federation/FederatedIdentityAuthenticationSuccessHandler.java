package ocudni104.idp.federation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ocudni104.idp.device.application.UpsertDeviceCommand;
import ocudni104.idp.device.application.UpsertDeviceUseCase;
import ocudni104.idp.device.domain.DeviceId;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final String DEVICE_ID_COOKIE_NAME = "did";
    private static final String DEVICE_OS_COOKIE_NAME = "device_os";
    private static final String DEVICE_SCREEN_COOKIE_NAME = "device_screen";

    private final AuthenticationSuccessHandler delegate;
    private final FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase;
    private final UpsertDeviceUseCase upsertDeviceUseCase;
    private final CreateSessionUseCase createSessionUseCase;
    private final String sessionCookieName;

    public FederatedIdentityAuthenticationSuccessHandler(
            String defaultTargetUrl,
            FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase,
            UpsertDeviceUseCase upsertDeviceUseCase,
            CreateSessionUseCase createSessionUseCase,
            String sessionCookieName
    ) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(defaultTargetUrl);
        this.delegate = handler;
        this.findOrCreateUserFromFederatedLoginUseCase = findOrCreateUserFromFederatedLoginUseCase;
        this.upsertDeviceUseCase = upsertDeviceUseCase;
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

            DeviceId deviceId = extractDeviceId(request);
            String deviceOs = readCookieValue(request, DEVICE_OS_COOKIE_NAME);
            String deviceScreen = readCookieValue(request, DEVICE_SCREEN_COOKIE_NAME);

            if (deviceId != null) {
                upsertDeviceUseCase.execute(new UpsertDeviceCommand(deviceId, deviceOs, deviceScreen));
            }

            Session session = createSessionUseCase.execute(new CreateSessionCommand(
                    user.id(),
                    null,
                    deviceId,
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
                    .secure(request.isSecure())
                    .maxAge(session.absoluteExpiresAt().getEpochSecond() - session.createdAt().getEpochSecond())
                    .build();
            response.addHeader("Set-Cookie", sidCookie.toString());
            clearTransientCookie(request, response, DEVICE_OS_COOKIE_NAME);
            clearTransientCookie(request, response, DEVICE_SCREEN_COOKIE_NAME);
        }

        delegate.onAuthenticationSuccess(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    static DeviceId extractDeviceId(HttpServletRequest request) {
        String value = readCookieValue(request, DEVICE_ID_COOKIE_NAME);
        if (value == null) {
            return null;
        }

        try {
            return new DeviceId(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static String readCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private static void clearTransientCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie", ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString());
    }
}
