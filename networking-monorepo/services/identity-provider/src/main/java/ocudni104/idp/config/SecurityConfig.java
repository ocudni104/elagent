package ocudni104.idp.config;

import ocudni104.idp.federation.FederatedIdentityAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {

    @Value("${app.frontend-url:http://localhost:4321}")
    private String frontendUrl;

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/.well-known/openid-configuration",
                    "/oauth2/jwks",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(new FederatedIdentityAuthenticationSuccessHandler(frontendUrl))
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
