package ptit.home_services_marketplace.request.dto;

import ptit.home_services_marketplace.request.ServiceRequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service request with its current content snapshot.
 */
public record ServiceRequestResponse(
        UUID id,
        ServiceRequestStatus status,
        Integer currentVersion,
        RequestVersionResponse details,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
