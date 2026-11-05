package ptit.home_services_marketplace.discovery;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.discovery.dto.EligibleProvidersResponse;
import ptit.home_services_marketplace.discovery.dto.ProviderSelectionResponse;
import ptit.home_services_marketplace.discovery.dto.SelectProviderRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

/**
 * Deterministic provider discovery and selection for confirmed service requests.
 */
@RestController
@RequestMapping("/api/v1")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/service-requests/{id}/eligible-providers")
    public EligibleProvidersResponse eligibleProviders(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @PathVariable UUID id) {
        return discoveryService.findEligibleProviders(user, id);
    }

    @PutMapping("/service-requests/{id}/provider-selection")
    public ProviderSelectionResponse selectProvider(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID id,
                                                    @RequestBody SelectProviderRequest request) {
        return discoveryService.selectProvider(user, id, request);
    }
}
