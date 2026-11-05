package ptit.home_services_marketplace.request.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of a request's content.
 */
public record RequestVersionResponse(
        Integer version,
        UUID categoryId,
        UUID serviceId,
        String description,
        String addressSnapshot,
        String areaCode,
        OffsetDateTime preferredStart,
        OffsetDateTime preferredEnd,
        BigDecimal budgetAmount,
        String budgetCurrency) {
}
