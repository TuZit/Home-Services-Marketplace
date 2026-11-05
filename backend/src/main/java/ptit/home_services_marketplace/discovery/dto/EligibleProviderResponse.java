package ptit.home_services_marketplace.discovery.dto;

import java.util.List;
import java.util.UUID;

/**
 * Public, secret-free view of an eligible provider plus the rules it satisfied and the
 * availability windows that matched the request.
 */
public record EligibleProviderResponse(
        UUID providerId,
        String businessName,
        String description,
        String experienceSummary,
        List<String> reasonCodes,
        List<AvailabilityWindowResponse> availabilityWindows) {
}
