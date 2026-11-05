package ptit.home_services_marketplace.provider.dto;

/**
 * Update payload for the authenticated provider's profile.
 */
public record UpdateProviderProfileRequest(
        String businessName,
        String description,
        String experienceSummary,
        String contactPhone) {
}
