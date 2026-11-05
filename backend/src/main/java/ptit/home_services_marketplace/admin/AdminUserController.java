package ptit.home_services_marketplace.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.admin.dto.AdminUserResponse;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> list() {
        return adminUserService.list();
    }

    @PostMapping("/{id}/suspend")
    public AdminUserResponse suspend(@PathVariable UUID id,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return adminUserService.suspend(id, user);
    }

    @PostMapping("/{id}/reactivate")
    public AdminUserResponse reactivate(@PathVariable UUID id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return adminUserService.reactivate(id, user);
    }
}
