package in.co.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class CustomUserService {

	private static final Logger log = LoggerFactory.getLogger(CustomUserService.class);

	/**
	 * Extracts authorities from JWT claims. No DB call — roles come directly from
	 * the token claims.
	 *
	 * Works for TWO token types: 1. Google JWT — no 'roles' claim → defaults to
	 * ROLE_USER 2. Your own JWT — has 'roles' claim → reads it directly
	 */
	public List<GrantedAuthority> processOAuth2User(String email, String name) {
		if (email == null || email.isBlank()) {
			log.warn("JWT missing email claim — rejecting");
			throw new IllegalArgumentException("JWT must contain a valid email claim");
		}
		log.debug("Resolved authority: email={}, role=ROLE_USER", email);

		// Default role — all authenticated Google users are ROLE_USER
		// To support ROLE_ADMIN: store privileged emails in application.yaml
		// and check here: adminEmails.contains(email) ? ROLE_ADMIN : ROLE_USER
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	/**
	 * Used by JwtAuthenticationConverter for YOUR OWN issued JWTs. Reads the
	 * 'roles' claim you put in during OAuth2SuccessHandler.
	 */
	public List<GrantedAuthority> extractAuthoritiesFromClaims(List<String> roles, String email) {

		if (roles == null || roles.isEmpty()) {
			log.debug("No roles claim in JWT for email={}, defaulting to ROLE_USER", email);
			return List.of(new SimpleGrantedAuthority("ROLE_USER"));
		}

		return roles.stream().map(SimpleGrantedAuthority::new).map(a -> (GrantedAuthority) a).toList();
	}
}