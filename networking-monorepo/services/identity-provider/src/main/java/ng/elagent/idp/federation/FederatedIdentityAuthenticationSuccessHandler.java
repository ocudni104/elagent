package ng.elagent.idp.federation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate;

    public FederatedIdentityAuthenticationSuccessHandler(String defaultTargetUrl) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(defaultTargetUrl);
        this.delegate = handler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email == null) {
                email = oauthToken.getPrincipal().getName();
            }

            UsernamePasswordAuthenticationToken localAuth = UsernamePasswordAuthenticationToken.authenticated(
                    email,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER")
            );

            SecurityContextHolder.getContext().setAuthentication(localAuth);
        }

        delegate.onAuthenticationSuccess(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
