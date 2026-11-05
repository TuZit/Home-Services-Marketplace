package ptit.home_services_marketplace.provider;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.provider.dto.AvailabilityWindow;
import ptit.home_services_marketplace.provider.dto.AvailabilityWindowRequest;
import ptit.home_services_marketplace.provider.dto.ProviderProfileResponse;
import ptit.home_services_marketplace.provider.dto.UpdateProviderProfileRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * Provider self-service endpoints. Requires the {@code PROVIDER} role.
 */
@RestController
@RequestMapping("/api/v1/provider")
public class ProviderController {

    private final ProviderManagementService providerService;

    public ProviderController(ProviderManagementService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/profile")
    public ProviderProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.getProfile(user);
    }

    @PutMapping("/profile")
    public ProviderProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestBody UpdateProviderProfileRequest request) {
        return providerService.updateProfile(user, request);
    }

    @GetMapping("/services")
    public List<UUID> listServices(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.listServiceIds(user);
    }

    @PutMapping("/services")
    public List<UUID> replaceServices(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestBody List<UUID> serviceIds) {
        return providerService.replaceServices(user, serviceIds);
    }

    @GetMapping("/areas")
    public List<String> listAreas(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.listAreaCodes(user);
    }

    @PutMapping("/areas")
    public List<String> replaceAreas(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestBody List<String> areaCodes) {
        return providerService.replaceAreas(user, areaCodes);
    }

    @GetMapping("/availability")
    public List<AvailabilityWindow> listAvailability(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.listAvailability(user);
    }

    @PutMapping("/availability")
    public List<AvailabilityWindow> replaceAvailability(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @RequestBody List<AvailabilityWindowRequest> windows) {
        return providerService.replaceAvailability(user, windows);
    }

    @PostMapping("/submit-verification")
    public ProviderProfileResponse submitVerification(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.submitVerification(user);
    }
}
