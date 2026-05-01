package in.co.service;

import in.co.dto.AppUser;
import in.co.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserService {

    // ✅ SLF4J logger — never use System.out.println in production code
    private static final Logger log = LoggerFactory.getLogger(CustomUserService.class);

    private final UserRepository userRepository;

    public CustomUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Syncs the Google OAuth2 user with the local database and returns their authorities.
     * Called on every authenticated request — result is cached by email to avoid
     * a DB round-trip on each API call.
     */
    @Cacheable(value = "userAuthorities", key = "#email")  // ✅ Cache per email
    @Transactional
    public List<GrantedAuthority> processOAuth2User(String email, String name) {

        // ✅ Guard against null/blank claims from malformed tokens
        if (email == null || email.isBlank()) {
            log.warn("Received JWT with missing email claim — rejecting");
            throw new IllegalArgumentException("JWT must contain a valid email claim");
        }

        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            // ✅ Structured log with context — easy to find in Kibana/Elasticsearch
            log.info("First login — provisioning new user: email={}", email);
            return userRepository.save(new AppUser(email, name, AppUser.Role.ROLE_USER));
        });

        log.debug("Resolved authorities for user: email={}, role={}", email, user.getRole());

        // ✅ Use enum name() — type-safe, no hardcoded strings
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }
}