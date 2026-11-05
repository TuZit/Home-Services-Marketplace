package ptit.home_services_marketplace.catalog.dto;

import java.util.UUID;

/**
 * Create/update payload for a service. {@code active} is optional; on create a missing
 * value defaults to {@code true}.
 */
public record ServiceUpsertRequest(
        UUID categoryId,
        String name,
        String slug,
        String description,
        Boolean active) {
}
