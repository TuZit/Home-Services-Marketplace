package ptit.home_services_marketplace.discovery.dto;

import java.util.List;
import java.util.UUID;

/**
 * Discovery result. When {@code providers} is empty, {@code noResultReason} explains the
 * rule at which candidates were exhausted.
 */
public record EligibleProvidersResponse(
        UUID requestId,
        Integer requestVersion,
        UUID discoveryRunId,
        String noResultReason,
        List<EligibleProviderResponse> providers) {
}
