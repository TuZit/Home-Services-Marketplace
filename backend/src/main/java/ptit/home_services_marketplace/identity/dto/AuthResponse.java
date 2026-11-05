package ptit.home_services_marketplace.identity.dto;

/**
 * Authentication result returned in the JSON body. The refresh token is never placed in the
 * body; it is delivered as a Secure HttpOnly {@code refresh_token} cookie.
 */
public record AuthResponse(String accessToken, UserResponse user) {
}
