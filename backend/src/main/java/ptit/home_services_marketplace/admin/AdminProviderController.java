package ptit.home_services_marketplace.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.admin.dto.AdminProviderResponse;
import ptit.home_services_marketplace.admin.dto.AdminReasonRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/providers")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    @PostMapping("/{id}/verify")
    public AdminProviderResponse verify(@PathVariable UUID id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return adminProviderService.verify(id, user);
    }

    @PostMapping("/{id}/reject")
    public AdminProviderResponse reject(@PathVariable UUID id,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestBody(required = false) AdminReasonRequest request) {
        return adminProviderService.reject(id, user, request == null ? null : request.reason());
    }

    @PostMapping("/{id}/suspend")
    public AdminProviderResponse suspend(@PathVariable UUID id,
                                         @AuthenticationPrincipal AuthenticatedUser user,
                                         @RequestBody(required = false) AdminReasonRequest request) {
        return adminProviderService.suspend(id, user, request == null ? null : request.reason());
    }
}
