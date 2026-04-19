package ocudni104.idp.config;

import ocudni104.idp.federation.FederatedIdentityAuthenticationSuccessHandler;
import ocudni104.idp.session.application.CreateSessionUseCase;
import ocudni104.idp.user.application.FindOrCreateUserFromFederatedLoginUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {

    @Value("${app.frontend-url:http://localhost:4321}")
    private String frontendUrl;

    @Value("${auth.session.cookie-name:sid}")
    private String sessionCookieName;

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            FindOrCreateUserFromFederatedLoginUseCase findOrCreateUserFromFederatedLoginUseCase,
            CreateSessionUseCase createSessionUseCase
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/.well-known/openid-configuration",
                    "/oauth2/jwks",
                    "/internal/token",
                    "/me",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .securityContext(securityContext -> securityContext
                .securityContextRepository(new NullSecurityContextRepository())
                .requireExplicitSave(false)
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(authorizationRequestRepository)
                )
                .successHandler(new FederatedIdentityAuthenticationSuccessHandler(
                        frontendUrl,
                        findOrCreateUserFromFederatedLoginUseCase,
                        createSessionUseCase,
                        sessionCookieName
                ))
            )
            // Internal API endpoints called by the gateway must return 401, not a login redirect,
            // and must never be saved as a post-login destination.
            .requestCache(cache -> cache
                .requestCache(new NullRequestCache())
            )
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/internal/")
                )
            );

        return http.build();
    }
}
