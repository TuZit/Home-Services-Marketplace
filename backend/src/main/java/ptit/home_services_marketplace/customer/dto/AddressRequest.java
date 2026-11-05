package ptit.home_services_marketplace.customer.dto;

import java.util.UUID;

/**
 * Create/update payload for a customer address. On update, {@code null} fields are left
 * unchanged.
 */
public record AddressRequest(
        String label,
        String addressLine,
        String areaCode,
        Boolean isDefault) {
}
