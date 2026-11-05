package ptit.home_services_marketplace.request;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.catalog.ServiceCategory;
import ptit.home_services_marketplace.catalog.ServiceCategoryRepository;
import ptit.home_services_marketplace.catalog.ServiceRepository;
import ptit.home_services_marketplace.customer.CustomerAddress;
import ptit.home_services_marketplace.customer.CustomerAddressRepository;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.discovery.ProviderSelection;
import ptit.home_services_marketplace.discovery.ProviderSelectionRepository;
import ptit.home_services_marketplace.discovery.ProviderSelectionStatus;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.request.dto.RequestVersionResponse;
import ptit.home_services_marketplace.request.dto.ServiceRequestResponse;
import ptit.home_services_marketplace.request.dto.ServiceRequestUpsertRequest;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.idempotency.IdempotencyService;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service-request use cases for the owning customer.
 *
 * <p>Versioning: the request keeps a single mutable draft snapshot while {@code DRAFT}.
 * Confirming the request materialises the immutable {@link RequestVersion} by incrementing
 * {@code current_version} and copying the draft into a frozen row, then locks the request by
 * moving it to {@code CONFIRMED}. Confirmation is idempotent, keyed on the optional
 * {@code Idempotency-Key} header.</p>
 */
@Service
public class RequestService {

    private static final String CONFIRM_OPERATION = "confirm-request";

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final RequestVersionRepository requestVersionRepository;
    private final ProviderSelectionRepository providerSelectionRepository;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public RequestService(CustomerProfileRepository customerProfileRepository,
                          CustomerAddressRepository customerAddressRepository,
                          ServiceCategoryRepository categoryRepository,
                          ServiceRepository serviceRepository,
                          ServiceRequestRepository serviceRequestRepository,
                          RequestVersionRepository requestVersionRepository,
                          ProviderSelectionRepository providerSelectionRepository,
                          IdempotencyService idempotencyService,
                          AuditService auditService) {
        this.customerProfileRepository = customerProfileRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.requestVersionRepository = requestVersionRepository;
        this.providerSelectionRepository = providerSelectionRepository;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    @Transactional
    public ServiceRequestResponse createDraft(AuthenticatedUser user, ServiceRequestUpsertRequest request) {
        CustomerProfile customer = resolveCustomer(user);

        if (request.categoryId() == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "categoryId is required");
        }
        if (request.serviceId() == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "serviceId is required");
        }
        ServiceCategory category = requireActiveCategory(request.categoryId());
        ptit.home_services_marketplace.catalog.Service service = requireActiveService(request.serviceId());
        requireServiceInCategory(service, category);
        CustomerAddress address = requireOwnAddress(customer, request.addressId());
        String description = requireText(request.description(), "description");
        OffsetDateTime start = requireInstant(request.preferredStart(), "preferredStart");
        OffsetDateTime end = requireInstant(request.preferredEnd(), "preferredEnd");
        requireOrderedWindow(start, end);
        String currency = normalizeCurrency(request.budgetCurrency(), "VND");

        ServiceRequest entity = new ServiceRequest();
        entity.setCustomerId(customer.getId());
        entity.setStatus(ServiceRequestStatus.DRAFT);
        entity.setCurrentVersion(1);
        serviceRequestRepository.save(entity);

        RequestVersion version = buildVersion(entity.getId(), 1, category.getId(), service.getId(),
                description, address.getAddressLine(), address.getAreaCode(), start, end,
                request.budgetAmount(), currency);
        requestVersionRepository.save(version);
        return toResponse(entity, version);
    }

    @Transactional
    public ServiceRequestResponse updateDraft(AuthenticatedUser user, UUID id, ServiceRequestUpsertRequest request) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest entity = loadOwned(customer, id);
        if (entity.getStatus() != ServiceRequestStatus.DRAFT) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Only draft requests can be edited");
        }
        RequestVersion current = currentVersion(entity);

        UUID categoryId = request.categoryId() != null ? request.categoryId() : current.getCategoryId();
        UUID serviceId = request.serviceId() != null ? request.serviceId() : current.getServiceId();
        ServiceCategory category = requireActiveCategory(categoryId);
        ptit.home_services_marketplace.catalog.Service service = requireActiveService(serviceId);
        requireServiceInCategory(service, category);

        String description = request.description() != null
                ? requireText(request.description(), "description")
                : current.getDescription();
        String addressSnapshot = current.getAddressSnapshot();
        String areaCode = current.getAreaCode();
        if (request.addressId() != null) {
            CustomerAddress address = requireOwnAddress(customer, request.addressId());
            addressSnapshot = address.getAddressLine();
            areaCode = address.getAreaCode();
        }
        OffsetDateTime start = request.preferredStart() != null ? request.preferredStart() : current.getPreferredStart();
        OffsetDateTime end = request.preferredEnd() != null ? request.preferredEnd() : current.getPreferredEnd();
        requireOrderedWindow(start, end);
        BigDecimal budget = request.budgetAmount() != null ? request.budgetAmount() : current.getBudgetAmount();
        String currency = normalizeCurrency(request.budgetCurrency(), current.getBudgetCurrency());

        // The draft snapshot is mutable; it is frozen as an immutable version on confirm.
        current.setCategoryId(category.getId());
        current.setServiceId(service.getId());
        current.setDescription(description);
        current.setAddressSnapshot(addressSnapshot);
        current.setAreaCode(areaCode);
        current.setPreferredStart(start);
        current.setPreferredEnd(end);
        current.setBudgetAmount(budget);
        current.setBudgetCurrency(currency);
        requestVersionRepository.save(current);
        return toResponse(entity, current);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getById(AuthenticatedUser user, UUID id) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest entity = loadOwned(customer, id);
        return toResponse(entity, currentVersion(entity));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listMyRequests(AuthenticatedUser user) {
        CustomerProfile customer = resolveCustomer(user);
        return serviceRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(entity -> toResponse(entity, currentVersion(entity)))
                .toList();
    }

    @Transactional
    public ServiceRequestResponse confirm(AuthenticatedUser user, UUID id, String idempotencyKey) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest entity = loadOwned(customer, id);

        IdempotencyService.Reservation reservation = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            reservation = idempotencyService.reserve(user.id(), CONFIRM_OPERATION, idempotencyKey, sha256Hex(id.toString()));
            if (reservation.replay()) {
                return idempotencyService.replayBody(reservation, ServiceRequestResponse.class);
            }
        }

        ServiceRequestResponse response;
        if (entity.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "A cancelled request cannot be confirmed");
        }
        if (entity.getStatus() == ServiceRequestStatus.DRAFT) {
            RequestVersion draft = currentVersion(entity);
            int frozenVersionNumber = entity.getCurrentVersion() + 1;
            RequestVersion frozen = copyAsVersion(draft, frozenVersionNumber);
            requestVersionRepository.save(frozen);

            entity.setCurrentVersion(frozenVersionNumber);
            entity.setStatus(ServiceRequestStatus.CONFIRMED);
            serviceRequestRepository.save(entity);

            auditService.record(user.id(), user.role().name(), "request.confirm", "service_request",
                    entity.getId(), new StatusSnapshot(ServiceRequestStatus.DRAFT, frozenVersionNumber - 1),
                    new StatusSnapshot(ServiceRequestStatus.CONFIRMED, frozenVersionNumber), null);
            response = toResponse(entity, frozen);
        } else {
            // Already confirmed (or advanced): idempotent no-op.
            response = toResponse(entity, currentVersion(entity));
        }

        if (reservation != null) {
            idempotencyService.complete(reservation.recordId(), response);
        }
        return response;
    }

    @Transactional
    public ServiceRequestResponse cancel(AuthenticatedUser user, UUID id) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest entity = loadOwned(customer, id);
        if (entity.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Request is already cancelled");
        }
        ServiceRequestStatus previous = entity.getStatus();
        entity.setStatus(ServiceRequestStatus.CANCELLED);
        serviceRequestRepository.save(entity);

        providerSelectionRepository.findByRequestIdAndStatus(entity.getId(), ProviderSelectionStatus.ACTIVE)
                .ifPresent(selection -> {
                    selection.setStatus(ProviderSelectionStatus.RELEASED);
                    providerSelectionRepository.save(selection);
                });

        auditService.record(user.id(), user.role().name(), "request.cancel", "service_request",
                entity.getId(), new StatusSnapshot(previous, entity.getCurrentVersion()),
                new StatusSnapshot(ServiceRequestStatus.CANCELLED, entity.getCurrentVersion()), null);
        return toResponse(entity, currentVersion(entity));
    }

    // ---------------------------------------------------------------------- helpers

    private CustomerProfile resolveCustomer(AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Authenticated account is not a customer");
        }
        return customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Customer profile not found"));
    }

    private ServiceRequest loadOwned(CustomerProfile customer, UUID id) {
        ServiceRequest entity = serviceRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service request not found"));
        if (!entity.getCustomerId().equals(customer.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Not the owner of this service request");
        }
        return entity;
    }

    private RequestVersion currentVersion(ServiceRequest entity) {
        return requestVersionRepository.findByRequestIdAndVersion(entity.getId(), entity.getCurrentVersion())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Request version not found"));
    }

    private CustomerAddress requireOwnAddress(CustomerProfile customer, UUID addressId) {
        if (addressId == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "addressId is required");
        }
        CustomerAddress address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown address"));
        if (!address.getCustomerId().equals(customer.getId()) || address.getDeletedAt() != null) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Address does not belong to the customer");
        }
        return address;
    }

    private ServiceCategory requireActiveCategory(UUID categoryId) {
        ServiceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown category"));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Category is not active");
        }
        return category;
    }

    private ptit.home_services_marketplace.catalog.Service requireActiveService(UUID serviceId) {
        ptit.home_services_marketplace.catalog.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown service"));
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Service is not active");
        }
        return service;
    }

    private void requireServiceInCategory(ptit.home_services_marketplace.catalog.Service service, ServiceCategory category) {
        if (!service.getCategoryId().equals(category.getId())) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Service does not belong to the category");
        }
    }

    private static RequestVersion buildVersion(UUID requestId, int version, UUID categoryId, UUID serviceId,
                                               String description, String addressSnapshot, String areaCode,
                                               OffsetDateTime start, OffsetDateTime end, BigDecimal budget,
                                               String currency) {
        RequestVersion entity = new RequestVersion();
        entity.setRequestId(requestId);
        entity.setVersion(version);
        entity.setCategoryId(categoryId);
        entity.setServiceId(serviceId);
        entity.setDescription(description);
        entity.setAddressSnapshot(addressSnapshot);
        entity.setAreaCode(areaCode);
        entity.setPreferredStart(start);
        entity.setPreferredEnd(end);
        entity.setBudgetAmount(budget);
        entity.setBudgetCurrency(currency);
        return entity;
    }

    private static RequestVersion copyAsVersion(RequestVersion source, int version) {
        return buildVersion(source.getRequestId(), version, source.getCategoryId(), source.getServiceId(),
                source.getDescription(), source.getAddressSnapshot(), source.getAreaCode(),
                source.getPreferredStart(), source.getPreferredEnd(), source.getBudgetAmount(),
                source.getBudgetCurrency());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, field + " is required");
        }
        return value.trim();
    }

    private static OffsetDateTime requireInstant(OffsetDateTime value, String field) {
        if (value == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, field + " is required");
        }
        return value;
    }

    private static void requireOrderedWindow(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "preferredStart must be before preferredEnd");
        }
    }

    private static String normalizeCurrency(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null || fallback.isBlank() ? "VND" : fallback;
        }
        String currency = value.trim().toUpperCase();
        if (currency.length() != 3) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "budgetCurrency must be a 3-letter code");
        }
        return currency;
    }

    private static RequestVersionResponse toVersionResponse(RequestVersion entity) {
        return new RequestVersionResponse(entity.getVersion(), entity.getCategoryId(), entity.getServiceId(),
                entity.getDescription(), entity.getAddressSnapshot(), entity.getAreaCode(),
                entity.getPreferredStart(), entity.getPreferredEnd(), entity.getBudgetAmount(),
                entity.getBudgetCurrency());
    }

    private static ServiceRequestResponse toResponse(ServiceRequest entity, RequestVersion version) {
        return new ServiceRequestResponse(entity.getId(), entity.getStatus(), entity.getCurrentVersion(),
                toVersionResponse(version), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private static String sha256Hex(String value) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Minimal before/after payload for audit summaries.
     */
    private record StatusSnapshot(ServiceRequestStatus status, Integer version) {
    }
}
