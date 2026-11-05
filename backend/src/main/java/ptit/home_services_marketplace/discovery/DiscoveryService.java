package ptit.home_services_marketplace.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.discovery.dto.AvailabilityWindowResponse;
import ptit.home_services_marketplace.discovery.dto.EligibleProviderResponse;
import ptit.home_services_marketplace.discovery.dto.EligibleProvidersResponse;
import ptit.home_services_marketplace.discovery.dto.ProviderSelectionResponse;
import ptit.home_services_marketplace.discovery.dto.SelectProviderRequest;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.provider.ProviderAvailability;
import ptit.home_services_marketplace.provider.ProviderProfile;
import ptit.home_services_marketplace.request.RequestVersion;
import ptit.home_services_marketplace.request.RequestVersionRepository;
import ptit.home_services_marketplace.request.ServiceRequest;
import ptit.home_services_marketplace.request.ServiceRequestRepository;
import ptit.home_services_marketplace.request.ServiceRequestStatus;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * Deterministic provider discovery and selection for the owning customer.
 *
 * <p>A discovery run is persisted (run + candidates) for every call to
 * {@code GET /service-requests/{id}/eligible-providers}. Selection re-validates eligibility
 * independently before recording the {@link ProviderSelection} and advancing the request.</p>
 */
@Service
public class DiscoveryService {

    private final CustomerProfileRepository customerProfileRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final RequestVersionRepository requestVersionRepository;
    private final DiscoveryRunRepository discoveryRunRepository;
    private final DiscoveryCandidateRepository discoveryCandidateRepository;
    private final ProviderSelectionRepository providerSelectionRepository;
    private final EligibilityEngine eligibilityEngine;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public DiscoveryService(CustomerProfileRepository customerProfileRepository,
                            ServiceRequestRepository serviceRequestRepository,
                            RequestVersionRepository requestVersionRepository,
                            DiscoveryRunRepository discoveryRunRepository,
                            DiscoveryCandidateRepository discoveryCandidateRepository,
                            ProviderSelectionRepository providerSelectionRepository,
                            EligibilityEngine eligibilityEngine,
                            ObjectMapper objectMapper,
                            AuditService auditService) {
        this.customerProfileRepository = customerProfileRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.requestVersionRepository = requestVersionRepository;
        this.discoveryRunRepository = discoveryRunRepository;
        this.discoveryCandidateRepository = discoveryCandidateRepository;
        this.providerSelectionRepository = providerSelectionRepository;
        this.eligibilityEngine = eligibilityEngine;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional
    public EligibleProvidersResponse findEligibleProviders(AuthenticatedUser user, UUID requestId) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest request = loadOwned(customer, requestId);
        if (request.getStatus() != ServiceRequestStatus.CONFIRMED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Discovery requires a CONFIRMED request");
        }
        RequestVersion version = currentVersion(request);
        EligibilityEngine.EligibilityOutcome outcome = eligibilityEngine.evaluate(version);

        DiscoveryRun run = new DiscoveryRun();
        run.setRequestId(request.getId());
        run.setRequestVersion(version.getVersion());
        run.setNoResultReason(outcome.noResultReason());
        discoveryRunRepository.save(run);

        for (EligibilityEngine.EligibleCandidate candidate : outcome.candidates()) {
            DiscoveryCandidate persisted = new DiscoveryCandidate();
            persisted.setDiscoveryRunId(run.getId());
            persisted.setProviderId(candidate.providerId());
            persisted.setReasonCodes(toJson(candidate.reasonCodes()));
            discoveryCandidateRepository.save(persisted);
        }

        List<EligibleProviderResponse> providers = outcome.candidates().stream()
                .map(this::toProviderResponse)
                .toList();
        return new EligibleProvidersResponse(request.getId(), version.getVersion(), run.getId(),
                outcome.noResultReason(), providers);
    }

    @Transactional
    public ProviderSelectionResponse selectProvider(AuthenticatedUser user, UUID requestId, SelectProviderRequest request) {
        CustomerProfile customer = resolveCustomer(user);
        ServiceRequest entity = loadOwned(customer, requestId);

        if (entity.getStatus() == ServiceRequestStatus.PROVIDER_SELECTED) {
            throw ApiException.conflict(ErrorCodes.ALREADY_SELECTED, "A provider is already selected");
        }
        if (entity.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "A cancelled request cannot select a provider");
        }
        if (entity.getStatus() != ServiceRequestStatus.CONFIRMED
                && entity.getStatus() != ServiceRequestStatus.PROVIDERS_FOUND) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "The request must be confirmed before selecting a provider");
        }
        if (request == null || request.providerId() == null) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "providerId is required");
        }
        if (providerSelectionRepository.findByRequestIdAndStatus(entity.getId(), ProviderSelectionStatus.ACTIVE).isPresent()) {
            throw ApiException.conflict(ErrorCodes.ALREADY_SELECTED, "A provider is already selected");
        }

        RequestVersion version = currentVersion(entity);
        eligibilityEngine.evaluateProvider(version, request.providerId())
                .orElseThrow(() -> ApiException.conflict(ErrorCodes.NOT_ELIGIBLE,
                        "Provider is not eligible for this request"));

        ProviderSelection selection = providerSelectionRepository
                .findByRequestIdAndRequestVersion(entity.getId(), version.getVersion())
                .orElseGet(ProviderSelection::new);
        selection.setRequestId(entity.getId());
        selection.setRequestVersion(version.getVersion());
        selection.setProviderId(request.providerId());
        selection.setSelectedBy(user.id());
        selection.setStatus(ProviderSelectionStatus.ACTIVE);
        providerSelectionRepository.save(selection);

        entity.setStatus(ServiceRequestStatus.PROVIDER_SELECTED);
        serviceRequestRepository.save(entity);

        auditService.record(user.id(), user.role().name(), "request.select-provider", "provider_selection",
                selection.getId(), null,
                new SelectionSnapshot(entity.getId(), request.providerId(), version.getVersion()), null);

        return toSelectionResponse(selection);
    }

    // ---------------------------------------------------------------------- helpers

    private CustomerProfile resolveCustomer(AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Authenticated account is not a customer");
        }
        return customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Customer profile not found"));
    }

    private ServiceRequest loadOwned(CustomerProfile customer, UUID requestId) {
        ServiceRequest entity = serviceRequestRepository.findById(requestId)
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

    private EligibleProviderResponse toProviderResponse(EligibilityEngine.EligibleCandidate candidate) {
        ProviderProfile profile = candidate.profile();
        List<AvailabilityWindowResponse> windows = candidate.matchingAvailability().stream()
                .map(DiscoveryService::toWindowResponse)
                .toList();
        return new EligibleProviderResponse(candidate.providerId(), profile.getBusinessName(),
                profile.getDescription(), profile.getExperienceSummary(), candidate.reasonCodes(), windows);
    }

    private static AvailabilityWindowResponse toWindowResponse(ProviderAvailability availability) {
        return new AvailabilityWindowResponse(availability.getDayOfWeek(), availability.getStartTime(),
                availability.getEndTime());
    }

    private static ProviderSelectionResponse toSelectionResponse(ProviderSelection selection) {
        return new ProviderSelectionResponse(selection.getId(), selection.getRequestId(),
                selection.getProviderId(), selection.getRequestVersion(), selection.getStatus(),
                selection.getSelectedAt());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record SelectionSnapshot(UUID requestId, UUID providerId, Integer requestVersion) {
    }
}
