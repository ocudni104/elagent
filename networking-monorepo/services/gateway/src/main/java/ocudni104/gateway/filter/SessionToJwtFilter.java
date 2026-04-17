package ocudni104.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Runs before Spring Security (-100). When a browser request carries a JSESSIONID cookie
 * but no Authorization header, this filter calls the IDP's /internal/token endpoint to
 * exchange the session for a short-lived JWT, then injects it as "Authorization: Bearer …"
 * so the downstream JWT resource-server validation works transparently.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionToJwtFilter implements WebFilter {

    private final WebClient idpClient;

    public SessionToJwtFilter(@Value("${app.idp.internal-uri:http://localhost:8081}") String idpUri) {
        this.idpClient = WebClient.builder().baseUrl(idpUri).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Skip IDP routes — they handle the OAuth2 login flow directly and must not have
        // their session touched here; doing so causes session fixation migration on the IDP
        // which clears the stored OAuth2 state and breaks the Google callback.
        if (exchange.getRequest().getPath().value().startsWith("/api/idp/")) {
            return chain.filter(exchange);
        }

        // Skip if a bearer token is already present
        if (exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return chain.filter(exchange);
        }

        var sessionCookie = exchange.getRequest().getCookies().getFirst("JSESSIONID");
        if (sessionCookie == null) {
            return chain.filter(exchange);
        }

        return idpClient.get()
                .uri("/internal/token")
                .cookie("JSESSIONID", sessionCookie.getValue())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(Map.class);
                    }
                    return response.releaseBody().thenReturn(Map.<String, Object>of());
                })
                .flatMap(body -> {
                    String token = (String) body.get("token");
                    if (token == null || token.isBlank()) {
                        return chain.filter(exchange);
                    }
                    var mutated = exchange.getRequest().mutate()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange))
                .onErrorResume(e -> chain.filter(exchange));
    }
}
