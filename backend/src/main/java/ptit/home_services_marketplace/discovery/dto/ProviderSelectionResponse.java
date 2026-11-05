package ptit.home_services_marketplace.discovery.dto;

import ptit.home_services_marketplace.discovery.ProviderSelectionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recorded provider selection.
 */
public record ProviderSelectionResponse(
        UUID id,
        UUID requestId,
        UUID providerId,
        Integer requestVersion,
        ProviderSelectionStatus status,
        OffsetDateTime selectedAt) {
}
