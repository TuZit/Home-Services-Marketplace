package ptit.home_services_marketplace.discovery.dto;

import java.util.UUID;

/**
 * Provider selection payload.
 */
public record SelectProviderRequest(UUID providerId) {
}
