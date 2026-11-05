package ptit.home_services_marketplace.admin;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.catalog.CatalogService;
import ptit.home_services_marketplace.catalog.dto.CategoryResponse;
import ptit.home_services_marketplace.catalog.dto.CategoryUpsertRequest;
import ptit.home_services_marketplace.catalog.dto.ServiceResponse;
import ptit.home_services_marketplace.catalog.dto.ServiceUpsertRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

/**
 * Admin catalogue maintenance. Deletion deactivates the row instead of removing it, so
 * historical references (requests, quotations, bookings) stay intact.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody CategoryUpsertRequest request) {
        return catalogService.createCategory(user, request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody CategoryUpsertRequest request) {
        return catalogService.updateCategory(user, id, request);
    }

    @DeleteMapping("/categories/{id}")
    public CategoryResponse deactivateCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable UUID id) {
        return catalogService.deactivateCategory(user, id);
    }

    @PostMapping("/services")
    public ServiceResponse createService(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody ServiceUpsertRequest request) {
        return catalogService.createService(user, request);
    }

    @PutMapping("/services/{id}")
    public ServiceResponse updateService(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody ServiceUpsertRequest request) {
        return catalogService.updateService(user, id, request);
    }

    @DeleteMapping("/services/{id}")
    public ServiceResponse deactivateService(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable UUID id) {
        return catalogService.deactivateService(user, id);
    }
}
