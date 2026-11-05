package ptit.home_services_marketplace.identity;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.identity.dto.AuthResponse;
import ptit.home_services_marketplace.identity.dto.LoginRequest;
import ptit.home_services_marketplace.identity.dto.RegisterRequest;

import java.time.Duration;

/**
 * Authentication endpoints. The access token is returned in the JSON body; the rotating
 * refresh token is delivered as a Secure HttpOnly SameSite=Lax cookie.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookiePath;
    private final Duration refreshTtl;

    public AuthController(AuthService authService,
                          @Value("${app.security.refresh-cookie.secure:true}") boolean cookieSecure,
                          @Value("${app.security.refresh-cookie.same-site:Lax}") String cookieSameSite,
                          @Value("${app.security.refresh-cookie.path:/api/v1/auth}") String cookiePath,
                          @Value("${app.jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookiePath = cookiePath;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        writeRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        writeRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                HttpServletResponse response) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        writeRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void writeRefreshCookie(HttpServletResponse response, String value) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(refreshTtl)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
