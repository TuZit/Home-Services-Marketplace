package ptit.home_services_marketplace.admin.dto;

import ptit.home_services_marketplace.identity.UserAccountStatus;
import ptit.home_services_marketplace.identity.UserRole;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        UserRole role,
        UserAccountStatus status) {
}
