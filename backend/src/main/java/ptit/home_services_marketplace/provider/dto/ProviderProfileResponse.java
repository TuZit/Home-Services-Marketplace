package ptit.home_services_marketplace.provider.dto;

import ptit.home_services_marketplace.provider.ProviderVerificationStatus;

import java.util.UUID;

/**
 * Provider profile as seen by its owner.
 */
public record ProviderProfileResponse(
        UUID id,
        String businessName,
        String description,
        String experienceSummary,
        String contactPhone,
        ProviderVerificationStatus verificationStatus) {
}
