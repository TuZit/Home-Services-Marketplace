package ptit.home_services_marketplace.shared.security;

import ptit.home_services_marketplace.identity.UserRole;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticated principal convention for all controllers.
 *
 * <p>Injected into controllers as {@code @AuthenticationPrincipal AuthenticatedUser user}
 * or read via {@link org.springframework.security.core.context.SecurityContextHolder}.</p>
 */
public record AuthenticatedUser(UUID id, UserRole role) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}
