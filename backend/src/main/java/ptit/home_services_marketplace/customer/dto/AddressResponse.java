package ptit.home_services_marketplace.customer.dto;

import java.util.UUID;

/**
 * Address-book entry as seen by the owner. Soft-deleted rows are never returned.
 */
public record AddressResponse(
        UUID id,
        String label,
        String addressLine,
        String areaCode,
        boolean isDefault) {
}
