package ptit.home_services_marketplace.customer.dto;

import java.util.UUID;

/**
 * Customer profile as seen by the owner.
 */
public record CustomerProfileResponse(UUID id, String fullName, String phone) {
}
