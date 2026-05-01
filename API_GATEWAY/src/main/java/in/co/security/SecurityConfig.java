package in.co.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import in.co.service.CustomUserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomUserService customUserService;

	public SecurityConfig(CustomUserService customUserService) {
		this.customUserService = customUserService;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(AbstractHttpConfigurer::disable)

				// ✅ STATELESS — JWTs are self-contained; no server-side session needed
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// ✅ Security headers — prevents clickjacking, sniffing, XSS
				.headers(headers -> headers.frameOptions(frame -> frame.deny()).contentTypeOptions(c -> {
				}).httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))

				.authorizeHttpRequests(authorize -> authorize
						// ✅ Explicit HTTP method on public endpoints is more precise
						.requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
						.requestMatchers("/eureka/**").permitAll().requestMatchers(HttpMethod.GET, "/caller/angular")
						.permitAll().requestMatchers(HttpMethod.GET, "/caller/**").permitAll()
						.requestMatchers("/caller/admin/**").hasRole("ADMIN")

						.anyRequest().authenticated())

				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
								// ✅ Custom 401 response instead of Spring's default redirect
								.authenticationEntryPoint((request, response, ex) -> {
									response.setStatus(401);
									response.setContentType("application/json");
									response.getWriter().write(
											"{\"error\":\"Unauthorized\",\"message\":\"Valid Bearer token required\"}");
								}));

		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> customUserService
				.processOAuth2User(jwt.getClaimAsString("email"), jwt.getClaimAsString("name")));
		return converter;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// ✅ In production, load this from application.yaml instead of hardcoding
		configuration.setAllowedOrigins(List.of("http://localhost:4200"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
		// ✅ Explicit headers instead of "*" — more secure and required when
		// credentials=true
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L); // ✅ Cache preflight for 1 hour — reduces OPTIONS requests

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}