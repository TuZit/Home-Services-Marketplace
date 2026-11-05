package ptit.home_services_marketplace.admin.dto;

import ptit.home_services_marketplace.provider.ProviderVerificationStatus;

import java.util.UUID;

public record AdminProviderResponse(
        UUID id,
        UUID userAccountId,
        String businessName,
        ProviderVerificationStatus verificationStatus) {
}
