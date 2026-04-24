package ocudni104.idp.federation;

import jakarta.servlet.http.Cookie;
import ocudni104.idp.config.CookieAuthorizationRequestRepository;
import ocudni104.idp.config.DeviceAwareOAuth2AuthorizationRequestResolver;
import ocudni104.idp.device.application.UpsertDeviceCommand;
import ocudni104.idp.device.application.UpsertDeviceUseCase;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.session.application.CreateSessionCommand;
import ocudni104.idp.session.application.CreateSessionUseCase;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.user.application.FindOrCreateUserFromFederatedLoginCommand;
import ocudni104.idp.user.application.FindOrCreateUserFromFederatedLoginUseCase;
import ocudni104.idp.user.domain.User;
import ocudni104.idp.user.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FederatedIdentityAuthenticationSuccessHandlerTest {

    @Mock
    private FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase;

    @Mock
    private CreateSessionUseCase createSessionUseCase;

    @Captor
    private ArgumentCaptor<CreateSessionCommand> createSessionCommandCaptor;

    @Captor
    private ArgumentCaptor<UpsertDeviceCommand> upsertDeviceCommandCaptor;

    @Mock
    private UpsertDeviceUseCase upsertDeviceUseCase;

    @Test
    void bindsSessionToDidCookieWhenPresent() throws Exception {
        UUID deviceId = UUID.randomUUID();
        FederatedIdentityAuthenticationSuccessHandler handler = new FederatedIdentityAuthenticationSuccessHandler(
                "http://localhost:4321/app",
                findOrCreateUserFromFederatedLoginUseCase,
                upsertDeviceUseCase,
                createSessionUseCase,
                "sid"
        );

        when(findOrCreateUserFromFederatedLoginUseCase.execute(any(FindOrCreateUserFromFederatedLoginCommand.class)))
                .thenReturn(user());
        when(createSessionUseCase.execute(any(CreateSessionCommand.class)))
                .thenReturn(session());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("did", deviceId.toString()));
        request.setAttribute(
                CookieAuthorizationRequestRepository.REQUEST_ATTRIBUTE_NAME,
                OAuth2AuthorizationRequest.authorizationCode()
                        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                        .clientId("google-client")
                        .redirectUri("http://localhost:8080/login/oauth2/code/google")
                        .state("oauth-state")
                        .authorizationRequestUri("https://accounts.google.com/o/oauth2/v2/auth?state=oauth-state")
                        .attributes(attributes -> {
                            attributes.put(DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_OS_ATTRIBUTE_NAME, "macOS");
                            attributes.put(DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_SCREEN_ATTRIBUTE_NAME, "1512x982@2");
                        })
                        .build()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        verify(upsertDeviceUseCase).execute(upsertDeviceCommandCaptor.capture());
        verify(createSessionUseCase).execute(createSessionCommandCaptor.capture());
        assertEquals(new UpsertDeviceCommand(new DeviceId(deviceId), "macOS", "1512x982@2"), upsertDeviceCommandCaptor.getValue());
        assertEquals(new DeviceId(deviceId), createSessionCommandCaptor.getValue().deviceId());
    }

    @Test
    void ignoresMalformedDidCookie() throws Exception {
        FederatedIdentityAuthenticationSuccessHandler handler = new FederatedIdentityAuthenticationSuccessHandler(
                "http://localhost:4321/app",
                findOrCreateUserFromFederatedLoginUseCase,
                upsertDeviceUseCase,
                createSessionUseCase,
                "sid"
        );

        when(findOrCreateUserFromFederatedLoginUseCase.execute(any(FindOrCreateUserFromFederatedLoginCommand.class)))
                .thenReturn(user());
        when(createSessionUseCase.execute(any(CreateSessionCommand.class)))
                .thenReturn(session());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("did", "not-a-uuid"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        verify(upsertDeviceUseCase, never()).execute(any(UpsertDeviceCommand.class));
        verify(createSessionUseCase).execute(createSessionCommandCaptor.capture());
        assertNull(createSessionCommandCaptor.getValue().deviceId());
    }

    private OAuth2AuthenticationToken authentication() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("SCOPE_openid"),
                        new SimpleGrantedAuthority("SCOPE_email")
                ),
                Map.of(
                        "email", "user@example.com",
                        "sub", "google-subject"
                ),
                "email"
        );

        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private User user() {
        Instant now = Instant.parse("2026-04-22T10:15:30Z");
        return User.builder()
                .id(new UserId(UUID.randomUUID()))
                .email("user@example.com")
                .provider("google")
                .providerSubject("google-subject")
                .createdAt(now)
                .lastLoginAt(now)
                .build();
    }

    private Session session() {
        Instant createdAt = Instant.parse("2026-04-22T10:15:30Z");
        return Session.builder()
                .id(SessionId.newId())
                .userId(new UserId(UUID.randomUUID()))
                .deviceId(null)
                .roles(Set.of("ROLE_USER"))
                .scopes(Set.of("openid", "email"))
                .createdAt(createdAt)
                .absoluteExpiresAt(createdAt.plusSeconds(3600))
                .build();
    }
}
