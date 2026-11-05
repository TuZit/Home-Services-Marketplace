package ptit.home_services_marketplace.request.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Create/update payload for a service request. {@code addressId} references one of the
 * caller's address-book entries; its content is snapshotted into the request version.
 */
public record ServiceRequestUpsertRequest(
        UUID categoryId,
        UUID serviceId,
        String description,
        UUID addressId,
        OffsetDateTime preferredStart,
        OffsetDateTime preferredEnd,
        BigDecimal budgetAmount,
        String budgetCurrency) {
}
