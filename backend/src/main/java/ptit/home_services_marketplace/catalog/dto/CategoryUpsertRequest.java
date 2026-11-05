package ptit.home_services_marketplace.catalog.dto;

/**
 * Create/update payload for a category. {@code active} is optional; on create a missing
 * value defaults to {@code true}.
 */
public record CategoryUpsertRequest(
        String name,
        String slug,
        String description,
        Boolean active) {
}
