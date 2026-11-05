package ptit.home_services_marketplace.identity.dto;

import ptit.home_services_marketplace.identity.UserAccountStatus;
import ptit.home_services_marketplace.identity.UserRole;

import java.util.UUID;

public record UserResponse(UUID id, String email, UserRole role, UserAccountStatus status) {
}
