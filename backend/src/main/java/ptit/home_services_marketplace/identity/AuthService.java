package ptit.home_services_marketplace.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.identity.dto.AuthResponse;
import ptit.home_services_marketplace.identity.dto.LoginRequest;
import ptit.home_services_marketplace.identity.dto.RegisterRequest;
import ptit.home_services_marketplace.identity.dto.UserResponse;
import ptit.home_services_marketplace.provider.ProviderProfile;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.JwtService;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Identity/authentication use cases: registration, login, refresh-token rotation and logout.
 *
 * <p>Refresh tokens are opaque random values; only their SHA-256 hash is persisted in
 * {@code refresh_token}. The raw value is returned to the controller which sets it as an
 * HttpOnly cookie.</p>
 */
@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TimeProvider timeProvider;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository userAccountRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       CustomerProfileRepository customerProfileRepository,
                       ProviderProfileRepository providerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TimeProvider timeProvider,
                       @Value("${app.jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.timeProvider = timeProvider;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (request.role() == null || request.role() == UserRole.ADMIN) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "role must be CUSTOMER or PROVIDER");
        }
        String emailNorm = normalizeEmail(request.email());
        if (emailNorm == null || emailNorm.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "password is required");
        }
        if (userAccountRepository.existsByEmailNorm(emailNorm)) {
            throw ApiException.conflict(ErrorCodes.EMAIL_EXISTS, "Email already registered");
        }

        UserAccount account = new UserAccount();
        account.setEmail(request.email().trim());
        account.setEmailNorm(emailNorm);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(request.role());
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.saveAndFlush(account);

        String displayName = (request.displayName() == null || request.displayName().isBlank())
                ? request.email().trim()
                : request.displayName().trim();
        if (request.role() == UserRole.CUSTOMER) {
            CustomerProfile profile = new CustomerProfile();
            profile.setUserAccountId(account.getId());
            profile.setFullName(displayName);
            customerProfileRepository.save(profile);
        } else {
            ProviderProfile profile = new ProviderProfile();
            profile.setUserAccountId(account.getId());
            profile.setBusinessName(displayName);
            providerProfileRepository.save(profile);
        }
        return issueTokens(account);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String emailNorm = normalizeEmail(request.email());
        UserAccount account = emailNorm == null
                ? null
                : userAccountRepository.findByEmailNorm(emailNorm).orElse(null);
        if (account == null || request.password() == null
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw ApiException.unauthorized(ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password");
        }
        if (account.getStatus() == UserAccountStatus.SUSPENDED) {
            throw ApiException.forbidden(ErrorCodes.ACCOUNT_SUSPENDED, "Account is suspended");
        }
        return issueTokens(account);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        RefreshToken stored = requireValidRefreshToken(rawRefreshToken);
        UserAccount account = userAccountRepository.findById(stored.getUserAccountId())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Invalid refresh token"));
        if (account.getStatus() == UserAccountStatus.SUSPENDED) {
            throw ApiException.forbidden(ErrorCodes.ACCOUNT_SUSPENDED, "Account is suspended");
        }

        String raw = newRefreshTokenValue();
        RefreshToken replacement = new RefreshToken();
        replacement.setUserAccountId(account.getId());
        replacement.setTokenHash(hash(raw));
        replacement.setExpiresAt(timeProvider.now().plus(refreshTtl));
        refreshTokenRepository.saveAndFlush(replacement);

        stored.setRevokedAt(timeProvider.now());
        stored.setReplacedBy(replacement.getId());
        refreshTokenRepository.save(stored);

        String accessToken = jwtService.generateAccessToken(account.getId(), account.getRole());
        return new AuthResult(new AuthResponse(accessToken, toResponse(account)), raw);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(timeProvider.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private RefreshToken requireValidRefreshToken(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Missing refresh token");
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(raw))
                .orElseThrow(() -> ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Invalid refresh token"));
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(timeProvider.now())) {
            throw ApiException.unauthorized(ErrorCodes.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        return token;
    }

    private AuthResult issueTokens(UserAccount account) {
        String accessToken = jwtService.generateAccessToken(account.getId(), account.getRole());
        String raw = newRefreshTokenValue();
        RefreshToken token = new RefreshToken();
        token.setUserAccountId(account.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(timeProvider.now().plus(refreshTtl));
        refreshTokenRepository.save(token);
        return new AuthResult(new AuthResponse(accessToken, toResponse(account)), raw);
    }

    private UserResponse toResponse(UserAccount account) {
        return new UserResponse(account.getId(), account.getEmail(), account.getRole(), account.getStatus());
    }

    private String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String hash(String value) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Result of an authentication use case: the body payload plus the raw refresh token to be
     * set as a cookie by the controller.
     */
    public record AuthResult(AuthResponse response, String refreshToken) {
    }
}
