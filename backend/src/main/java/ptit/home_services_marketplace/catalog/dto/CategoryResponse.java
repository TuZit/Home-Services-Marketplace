package ptit.home_services_marketplace.catalog.dto;

import java.util.UUID;

/**
 * Public representation of a service category. Never exposes internal audit columns.
 */
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active) {
}
