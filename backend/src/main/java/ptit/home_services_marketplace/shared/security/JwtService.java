package ptit.home_services_marketplace.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generates and parses HS256 JSON Web Tokens.
 *
 * <p>Subject = user id, custom claim {@code role} = {@link UserRole} name. The signing
 * secret and access token TTL are read from {@code app.jwt.*}.</p>
 */
@Component
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration accessTtl;
    private final TimeProvider timeProvider;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-ttl-minutes:15}") long accessTtlMinutes,
                      TimeProvider timeProvider) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.timeProvider = timeProvider;
    }

    public String generateAccessToken(UUID userId, UserRole role) {
        Instant issuedAt = timeProvider.now().toInstant();
        Instant expiresAt = issuedAt.plus(accessTtl);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and verifies a signed access token.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, invalid or expired
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        UUID userId = UUID.fromString(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get(ROLE_CLAIM, String.class));
        return new AuthenticatedUser(userId, role);
    }
}
