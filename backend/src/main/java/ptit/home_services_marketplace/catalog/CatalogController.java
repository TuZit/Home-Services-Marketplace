package ptit.home_services_marketplace.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.catalog.dto.CategoryResponse;
import ptit.home_services_marketplace.catalog.dto.GeoAreaResponse;
import ptit.home_services_marketplace.catalog.dto.ServiceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Public catalogue endpoints (no authentication required).
 */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalogService.listActiveCategories();
    }

    @GetMapping("/categories/{id}/services")
    public List<ServiceResponse> listCategoryServices(@PathVariable UUID id) {
        return catalogService.listActiveServices(id);
    }

    @GetMapping("/services/{id}")
    public ServiceResponse getService(@PathVariable UUID id) {
        return catalogService.getService(id);
    }

    @GetMapping("/areas")
    public List<GeoAreaResponse> listAreas() {
        return catalogService.listActiveAreas();
    }
}
