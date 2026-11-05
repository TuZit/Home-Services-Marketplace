package ptit.home_services_marketplace.identity.dto;

import ptit.home_services_marketplace.identity.UserRole;

/**
 * Registration payload. {@code role} must be CUSTOMER or PROVIDER. {@code displayName} is
 * mapped to {@code customer_profile.full_name} (CUSTOMER) or {@code provider_profile.business_name}
 * (PROVIDER).
 */
public record RegisterRequest(String email, String password, UserRole role, String displayName) {
}
