package ptit.home_services_marketplace.catalog;

import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.catalog.dto.CategoryResponse;
import ptit.home_services_marketplace.catalog.dto.CategoryUpsertRequest;
import ptit.home_services_marketplace.catalog.dto.GeoAreaResponse;
import ptit.home_services_marketplace.catalog.dto.ServiceResponse;
import ptit.home_services_marketplace.catalog.dto.ServiceUpsertRequest;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Catalogue use cases: public browsing of active categories/services and admin
 * maintenance. Deletion is a soft "deactivate" (the {@code active} flag) — no hard delete.
 */
@org.springframework.stereotype.Service
public class CatalogService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final GeoAreaRepository geoAreaRepository;
    private final AuditService auditService;

    public CatalogService(ServiceCategoryRepository categoryRepository,
                          ServiceRepository serviceRepository,
                          GeoAreaRepository geoAreaRepository,
                          AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.geoAreaRepository = geoAreaRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ public reads

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CatalogService::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listActiveServices(UUID categoryId) {
        ServiceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Category not found"));
        return serviceRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(category.getId()).stream()
                .map(CatalogService::toServiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(UUID id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service not found"));
        return toServiceResponse(service);
    }

    @Transactional(readOnly = true)
    public List<GeoAreaResponse> listActiveAreas() {
        return geoAreaRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(CatalogService::toGeoAreaResponse)
                .toList();
    }

    // ------------------------------------------------------------------- admin writes

    @Transactional
    public CategoryResponse createCategory(AuthenticatedUser actor, CategoryUpsertRequest request) {
        String name = requireText(request.name(), "name");
        String slug = slugify(request.slug(), name);
        if (categoryRepository.existsBySlug(slug)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Category slug already exists");
        }
        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(request.description()));
        category.setActive(request.active() == null || request.active());
        categoryRepository.save(category);
        auditService.record(actor.id(), actor.role().name(), "catalog.category.create",
                "service_category", category.getId(), null, toCategoryResponse(category), null);
        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(AuthenticatedUser actor, UUID id, CategoryUpsertRequest request) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Category not found"));
        CategoryResponse before = toCategoryResponse(category);

        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name().trim());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = slugify(request.slug(), category.getName());
            if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
                throw ApiException.conflict(ErrorCodes.CONFLICT, "Category slug already exists");
            }
            category.setSlug(slug);
        }
        if (request.description() != null) {
            category.setDescription(trimToNull(request.description()));
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }
        categoryRepository.save(category);
        auditService.record(actor.id(), actor.role().name(), "catalog.category.update",
                "service_category", category.getId(), before, toCategoryResponse(category), null);
        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse deactivateCategory(AuthenticatedUser actor, UUID id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Category not found"));
        CategoryResponse before = toCategoryResponse(category);
        category.setActive(false);
        categoryRepository.save(category);
        auditService.record(actor.id(), actor.role().name(), "catalog.category.deactivate",
                "service_category", category.getId(), before, toCategoryResponse(category), null);
        return toCategoryResponse(category);
    }

    @Transactional
    public ServiceResponse createService(AuthenticatedUser actor, ServiceUpsertRequest request) {
        UUID categoryId = request.categoryId();
        if (categoryId == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "categoryId is required");
        }
        ServiceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown category"));
        String name = requireText(request.name(), "name");
        String slug = slugify(request.slug(), name);
        if (serviceRepository.existsBySlug(slug)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Service slug already exists");
        }
        Service service = new Service();
        service.setCategoryId(category.getId());
        service.setName(name);
        service.setSlug(slug);
        service.setDescription(trimToNull(request.description()));
        service.setActive(request.active() == null || request.active());
        serviceRepository.save(service);
        auditService.record(actor.id(), actor.role().name(), "catalog.service.create",
                "service", service.getId(), null, toServiceResponse(service), null);
        return toServiceResponse(service);
    }

    @Transactional
    public ServiceResponse updateService(AuthenticatedUser actor, UUID id, ServiceUpsertRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service not found"));
        ServiceResponse before = toServiceResponse(service);

        if (request.categoryId() != null && !request.categoryId().equals(service.getCategoryId())) {
            ServiceCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown category"));
            service.setCategoryId(category.getId());
        }
        if (request.name() != null && !request.name().isBlank()) {
            service.setName(request.name().trim());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = slugify(request.slug(), service.getName());
            if (!slug.equals(service.getSlug()) && serviceRepository.existsBySlug(slug)) {
                throw ApiException.conflict(ErrorCodes.CONFLICT, "Service slug already exists");
            }
            service.setSlug(slug);
        }
        if (request.description() != null) {
            service.setDescription(trimToNull(request.description()));
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }
        serviceRepository.save(service);
        auditService.record(actor.id(), actor.role().name(), "catalog.service.update",
                "service", service.getId(), before, toServiceResponse(service), null);
        return toServiceResponse(service);
    }

    @Transactional
    public ServiceResponse deactivateService(AuthenticatedUser actor, UUID id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service not found"));
        ServiceResponse before = toServiceResponse(service);
        service.setActive(false);
        serviceRepository.save(service);
        auditService.record(actor.id(), actor.role().name(), "catalog.service.deactivate",
                "service", service.getId(), before, toServiceResponse(service), null);
        return toServiceResponse(service);
    }

    // ---------------------------------------------------------------------- helpers

    static CategoryResponse toCategoryResponse(ServiceCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), Boolean.TRUE.equals(category.getActive()));
    }

    static ServiceResponse toServiceResponse(Service service) {
        return new ServiceResponse(service.getId(), service.getCategoryId(), service.getName(),
                service.getSlug(), service.getDescription(), Boolean.TRUE.equals(service.getActive()));
    }

    static GeoAreaResponse toGeoAreaResponse(GeoArea area) {
        return new GeoAreaResponse(area.getCode(), area.getName(), area.getType(), area.getParentCode());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String slugify(String slug, String fallbackName) {
        String source = (slug == null || slug.isBlank()) ? fallbackName : slug;
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (normalized.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "slug could not be derived");
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }
}
