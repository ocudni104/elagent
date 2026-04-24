package ocudni104.idp.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeviceAwareOAuth2AuthorizationRequestResolverTest {

    @Test
    void storesSanitizedDeviceMetadataInAuthorizationRequestAttributes() {
        DeviceAwareOAuth2AuthorizationRequestResolver resolver =
                new DeviceAwareOAuth2AuthorizationRequestResolver(clientRegistrationRepository());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorization/google"
        );
        request.addParameter("deviceOs", "  Mac   OS  ");
        request.addParameter("deviceScreen", " 1512x982@2 ");

        OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);

        assertEquals("Mac OS", authorizationRequest.getAttribute(
                DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_OS_ATTRIBUTE_NAME
        ));
        assertEquals("1512x982@2", authorizationRequest.getAttribute(
                DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_SCREEN_ATTRIBUTE_NAME
        ));
    }

    @Test
    void leavesAuthorizationRequestUntouchedWhenDeviceMetadataMissing() {
        DeviceAwareOAuth2AuthorizationRequestResolver resolver =
                new DeviceAwareOAuth2AuthorizationRequestResolver(clientRegistrationRepository());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorization/google"
        );

        OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);

        assertNull(authorizationRequest.getAttribute(
                DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_OS_ATTRIBUTE_NAME
        ));
        assertNull(authorizationRequest.getAttribute(
                DeviceAwareOAuth2AuthorizationRequestResolver.DEVICE_SCREEN_ATTRIBUTE_NAME
        ));
    }

    private InMemoryClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("google-client")
                .clientSecret("google-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .scope("openid", "email", "profile")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
