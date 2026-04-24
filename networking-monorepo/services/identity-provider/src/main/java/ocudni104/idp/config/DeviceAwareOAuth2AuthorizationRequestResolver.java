package ocudni104.idp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class DeviceAwareOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    public static final String DEVICE_OS_ATTRIBUTE_NAME = "deviceOs";
    public static final String DEVICE_SCREEN_ATTRIBUTE_NAME = "deviceScreen";

    private static final int DEVICE_OS_MAX_LENGTH = 120;
    private static final int DEVICE_SCREEN_MAX_LENGTH = 64;

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public DeviceAwareOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return enrich(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return enrich(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest enrich(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        String deviceOs = sanitize(request.getParameter("deviceOs"), DEVICE_OS_MAX_LENGTH);
        String deviceScreen = sanitize(request.getParameter("deviceScreen"), DEVICE_SCREEN_MAX_LENGTH);

        if (deviceOs == null && deviceScreen == null) {
            return authorizationRequest;
        }

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);

        builder.attributes(attributes -> {
            if (deviceOs != null) {
                attributes.put(DEVICE_OS_ATTRIBUTE_NAME, deviceOs);
            }

            if (deviceScreen != null) {
                attributes.put(DEVICE_SCREEN_ATTRIBUTE_NAME, deviceScreen);
            }
        });

        return builder.build();
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }
}
