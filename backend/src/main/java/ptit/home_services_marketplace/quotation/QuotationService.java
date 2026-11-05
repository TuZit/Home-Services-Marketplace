package ptit.home_services_marketplace.quotation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.booking.Booking;
import ptit.home_services_marketplace.booking.BookingService;
import ptit.home_services_marketplace.booking.dto.BookingResponse;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.discovery.ProviderSelection;
import ptit.home_services_marketplace.discovery.ProviderSelectionRepository;
import ptit.home_services_marketplace.discovery.ProviderSelectionStatus;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.provider.ProviderProfile;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.provider.ProviderVerificationStatus;
import ptit.home_services_marketplace.quotation.dto.CreateQuotationRequest;
import ptit.home_services_marketplace.quotation.dto.QuotationItemResponse;
import ptit.home_services_marketplace.quotation.dto.QuotationResponse;
import ptit.home_services_marketplace.request.ServiceRequest;
import ptit.home_services_marketplace.request.ServiceRequestRepository;
import ptit.home_services_marketplace.request.ServiceRequestStatus;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.idempotency.IdempotencyService;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Quotation use cases: draft creation by the selected provider, submit / withdraw by the
 * provider, and accept / reject by the owning customer.
 *
 * <p>{@link #accept} is the single consequential command: it locks the quotation row, then
 * atomically converts the selection, creates the booking and reserves the provider slot.</p>
 */
@Service
public class QuotationService {

    private static final String ACCEPT_OPERATION = "accept-quotation";

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final ProviderSelectionRepository providerSelectionRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final BookingService bookingService;
    private final IdempotencyService idempotencyService;
    private final TimeProvider timeProvider;

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationItemRepository quotationItemRepository,
                            ProviderSelectionRepository providerSelectionRepository,
                            ServiceRequestRepository serviceRequestRepository,
                            CustomerProfileRepository customerProfileRepository,
                            ProviderProfileRepository providerProfileRepository,
                            BookingService bookingService,
                            IdempotencyService idempotencyService,
                            TimeProvider timeProvider) {
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.providerSelectionRepository = providerSelectionRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.bookingService = bookingService;
        this.idempotencyService = idempotencyService;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public QuotationResponse createDraft(UUID requestId, AuthenticatedUser user,
                                         CreateQuotationRequest request) {
        ProviderProfile provider = requireProvider(user);
        validateCreateRequest(request);

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service request not found"));
        if (serviceRequest.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Service request is cancelled");
        }

        ProviderSelection selection = providerSelectionRepository
                .findByRequestIdAndRequestVersion(requestId, serviceRequest.getCurrentVersion())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND,
                        "No provider selection exists for the current request version"));
        if (!selection.getProviderId().equals(provider.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN,
                    "Only the selected provider may quote this request");
        }
        if (selection.getStatus() != ProviderSelectionStatus.ACTIVE) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Provider selection is no longer active");
        }

        Quotation quotation = new Quotation();
        quotation.setSelectionId(selection.getId());
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setTotal(request.total());
        quotation.setCurrency("VND");
        quotation.setServiceStart(request.serviceStart());
        quotation.setServiceEnd(request.serviceEnd());
        quotation.setValidUntil(request.validUntil());
        quotationRepository.save(quotation);

        List<QuotationItem> items = new ArrayList<>();
        for (CreateQuotationRequest.Item item : request.items()) {
            QuotationItem entity = new QuotationItem();
            entity.setQuotationId(quotation.getId());
            entity.setDescription(item.description());
            entity.setAmount(item.amount());
            entity.setCurrency("VND");
            items.add(entity);
        }
        quotationItemRepository.saveAll(items);

        return toResponse(quotation, items);
    }

    @Transactional
    public QuotationResponse submit(UUID quotationId, AuthenticatedUser user) {
        ProviderProfile provider = requireProvider(user);
        Quotation quotation = require(quotationId);
        requireOwningSelectionProvider(quotation, provider);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Only a draft quotation can be submitted");
        }
        quotation.setStatus(QuotationStatus.SUBMITTED);
        try {
            quotationRepository.saveAndFlush(quotation);
        } catch (DataIntegrityViolationException ex) {
            // Violates uq_quote_active_selection: at most one SUBMITTED quotation per selection.
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Another submitted quotation already exists for this provider selection");
        }
        return toResponse(quotation);
    }

    @Transactional
    public QuotationResponse withdraw(UUID quotationId, AuthenticatedUser user) {
        ProviderProfile provider = requireProvider(user);
        Quotation quotation = require(quotationId);
        requireOwningSelectionProvider(quotation, provider);
        if (quotation.getStatus() != QuotationStatus.DRAFT
                && quotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Only a draft or submitted quotation can be withdrawn");
        }
        quotation.setStatus(QuotationStatus.WITHDRAWN);
        quotationRepository.save(quotation);
        return toResponse(quotation);
    }

    @Transactional
    public QuotationResponse reject(UUID quotationId, AuthenticatedUser user) {
        Quotation quotation = require(quotationId);
        requireOwningCustomer(quotation, user);
        if (quotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Only a submitted quotation can be rejected");
        }
        quotation.setStatus(QuotationStatus.REJECTED);
        quotationRepository.save(quotation);
        return toResponse(quotation);
    }

    @Transactional(readOnly = true)
    public QuotationResponse get(UUID quotationId, AuthenticatedUser user) {
        Quotation quotation = require(quotationId);
        boolean allowed = false;
        if (user.role() == UserRole.ADMIN) {
            allowed = true;
        } else if (user.role() == UserRole.PROVIDER) {
            ProviderProfile provider = providerProfileRepository.findByUserAccountId(user.id()).orElse(null);
            allowed = provider != null && selectionFor(quotation).getProviderId().equals(provider.getId());
        } else if (user.role() == UserRole.CUSTOMER) {
            allowed = customerIdFor(quotation).equals(ownerCustomerId(user));
        }
        if (!allowed) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You are not allowed to view this quotation");
        }
        return toResponse(quotation);
    }

    /**
     * Accepts a SUBMITTED quotation for the owning customer. Idempotent on the
     * {@code accept-quotation} operation. See the class-level note for ordering.
     */
    @Transactional
    public BookingResponse accept(UUID quotationId, AuthenticatedUser user, String idempotencyKey) {
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? quotationId.toString()
                : idempotencyKey;
        String requestHash = sha256Hex(user.id() + "|" + quotationId);
        IdempotencyService.Reservation reservation =
                idempotencyService.reserve(user.id(), ACCEPT_OPERATION, key, requestHash);
        if (reservation.replay()) {
            return idempotencyService.replayBody(reservation, BookingResponse.class);
        }

        Quotation quotation = quotationRepository.findByIdWithLock(quotationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Quotation not found"));
        if (quotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Only a submitted quotation can be accepted");
        }
        if (quotation.getValidUntil() != null && quotation.getValidUntil().isBefore(timeProvider.now())) {
            throw ApiException.conflict(ErrorCodes.EXPIRED, "Quotation has expired");
        }

        ProviderSelection selection = selectionFor(quotation);
        ServiceRequest serviceRequest = serviceRequestRepository.findById(selection.getRequestId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service request not found"));

        CustomerProfile customer = customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.FORBIDDEN,
                        "Only the owning customer may accept this quotation"));
        if (!serviceRequest.getCustomerId().equals(customer.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN,
                    "Only the owning customer may accept this quotation");
        }

        if (selection.getStatus() != ProviderSelectionStatus.ACTIVE) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Provider selection is no longer active");
        }
        if (selection.getRequestVersion() == null
                || !selection.getRequestVersion().equals(serviceRequest.getCurrentVersion())) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Provider selection is for a stale request version");
        }
        ProviderProfile provider = providerProfileRepository.findById(selection.getProviderId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Provider not found"));
        if (provider.getVerificationStatus() != ProviderVerificationStatus.VERIFIED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Provider is not verified");
        }
        if (bookingService.hasBookingForQuotation(quotationId)) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Quotation has already been accepted");
        }

        Booking booking = bookingService.createConfirmedBooking(
                quotation,
                provider.getId(),
                serviceRequest.getCustomerId(),
                quotation.getServiceStart(),
                quotation.getServiceEnd());

        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotationRepository.save(quotation);

        selection.setStatus(ProviderSelectionStatus.CONVERTED);
        providerSelectionRepository.save(selection);

        if (serviceRequest.getStatus() != ServiceRequestStatus.CANCELLED
                && serviceRequest.getStatus() != ServiceRequestStatus.PROVIDER_SELECTED) {
            serviceRequest.setStatus(ServiceRequestStatus.PROVIDER_SELECTED);
            serviceRequestRepository.save(serviceRequest);
        }

        BookingResponse response = bookingService.toResponse(booking);
        idempotencyService.complete(reservation.recordId(), response);
        return response;
    }

    public QuotationResponse toResponse(Quotation quotation) {
        return toResponse(quotation, quotationItemRepository.findByQuotationId(quotation.getId()));
    }

    private QuotationResponse toResponse(Quotation quotation, List<QuotationItem> items) {
        List<QuotationItemResponse> itemResponses = items.stream()
                .map(item -> new QuotationItemResponse(
                        item.getId(), item.getDescription(), item.getAmount(), item.getCurrency()))
                .toList();
        return new QuotationResponse(
                quotation.getId(),
                quotation.getSelectionId(),
                quotation.getStatus(),
                quotation.getTotal(),
                quotation.getCurrency(),
                quotation.getServiceStart(),
                quotation.getServiceEnd(),
                quotation.getValidUntil(),
                quotation.getCreatedAt(),
                itemResponses);
    }

    private Quotation require(UUID quotationId) {
        return quotationRepository.findById(quotationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Quotation not found"));
    }

    private ProviderSelection selectionFor(Quotation quotation) {
        return providerSelectionRepository.findById(quotation.getSelectionId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Provider selection not found"));
    }

    private UUID customerIdFor(Quotation quotation) {
        ProviderSelection selection = selectionFor(quotation);
        ServiceRequest request = serviceRequestRepository.findById(selection.getRequestId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Service request not found"));
        return request.getCustomerId();
    }

    private UUID ownerCustomerId(AuthenticatedUser user) {
        return customerProfileRepository.findByUserAccountId(user.id())
                .map(CustomerProfile::getId)
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer profile not found"));
    }

    private ProviderProfile requireProvider(AuthenticatedUser user) {
        if (user.role() != UserRole.PROVIDER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Provider role required");
        }
        return providerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.FORBIDDEN, "Provider profile not found"));
    }

    private void requireOwningSelectionProvider(Quotation quotation, ProviderProfile provider) {
        if (!selectionFor(quotation).getProviderId().equals(provider.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN,
                    "Only the selected provider may modify this quotation");
        }
    }

    private void requireOwningCustomer(Quotation quotation, AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer role required");
        }
        if (!customerIdFor(quotation).equals(ownerCustomerId(user))) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN,
                    "Only the owning customer may act on this quotation");
        }
    }

    private void validateCreateRequest(CreateQuotationRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "At least one quotation item is required");
        }
        if (request.total() == null || request.total().signum() < 0) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "total is required and must be non-negative");
        }
        if (request.serviceStart() == null || request.serviceEnd() == null
                || !request.serviceStart().isBefore(request.serviceEnd())) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "serviceStart must be before serviceEnd");
        }
        for (CreateQuotationRequest.Item item : request.items()) {
            if (item == null || item.description() == null || item.description().isBlank()
                    || item.amount() == null || item.amount().signum() < 0) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION,
                        "Each item requires a description and a non-negative amount");
            }
        }
    }

    private String sha256Hex(String value) {
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
}
