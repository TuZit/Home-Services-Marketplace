package ptit.home_services_marketplace.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.admin.dto.AdminReviewResponse;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public List<AdminReviewResponse> list() {
        return adminReviewService.list();
    }

    @PostMapping("/{id}/hide")
    public AdminReviewResponse hide(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return adminReviewService.hide(id, user);
    }

    @PostMapping("/{id}/restore")
    public AdminReviewResponse restore(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return adminReviewService.restore(id, user);
    }
}
