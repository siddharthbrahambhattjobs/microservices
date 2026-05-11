package in.co.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import in.co.service.JwtIssuerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final JwtIssuerService jwtIssuerService;
    private final String frontendUrl;

    // CustomUserService NOT needed here anymore — no DB lookup
    public OAuth2SuccessHandler(
            JwtIssuerService jwtIssuerService,
            @Value("${app.frontend.url:http://localhost:4200}") String frontendUrl) {
        this.jwtIssuerService = jwtIssuerService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        String email = oauthToken.getPrincipal().getAttribute("email");
        String name  = oauthToken.getPrincipal().getAttribute("name");

        log.info("OAuth2 login success: email={}", email);

        // No DB — default role for all Google-authenticated users
        List<String> roles = List.of("ROLE_USER");

        // Issue your own short-lived JWT
        String appJwt = jwtIssuerService.generateToken(email, name, roles);

        // Redirect Angular — fragment (#) never sent to server, safer than ?token=
        response.sendRedirect(frontendUrl + "/auth-callback#token=" + appJwt);
    }
}