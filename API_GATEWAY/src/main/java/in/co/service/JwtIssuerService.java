package in.co.service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtIssuerService {

    private final SecretKey signingKey;
    private final long expiryMillis;

    public JwtIssuerService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiryMillis = expiryMinutes * 60 * 1000L;
    }

    public String generateToken(String email, String name, List<String> roles) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .issuer("apigateway-server")
                .claim("email", email)
                .claim("name", name)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMillis)))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }
}