package ptit.home_services_marketplace.catalog.dto;

import java.util.UUID;

/**
 * Public representation of a catalogue service.
 */
public record ServiceResponse(
        UUID id,
        UUID categoryId,
        String name,
        String slug,
        String description,
        boolean active) {
}
