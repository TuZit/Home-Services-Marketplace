package ptit.home_services_marketplace.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.booking.dto.BookingResponse;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.quotation.Quotation;
import ptit.home_services_marketplace.scheduling.SchedulingService;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Booking lifecycle: creation (invoked from {@code QuotationService.accept}), reads and the
 * start / complete / cancel transitions. Cancellation follows the actor matrix in
 * Data Model v0.1 §6 and always releases the ACTIVE slot reservation.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final SchedulingService schedulingService;
    private final AuditService auditService;

    public BookingService(BookingRepository bookingRepository,
                          CustomerProfileRepository customerProfileRepository,
                          ProviderProfileRepository providerProfileRepository,
                          SchedulingService schedulingService,
                          AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.schedulingService = schedulingService;
        this.auditService = auditService;
    }

    /**
     * Inserts a CONFIRMED booking for an accepted quotation and immediately reserves the
     * provider slot. Runs inside the caller's accept transaction.
     *
     * <p>The slot reservation references {@code booking(id)}, so the booking row is inserted
     * (and flushed) first and the reservation second; both share one transaction, so a slot
     * conflict rolls the booking back.</p>
     */
    @Transactional
    public Booking createConfirmedBooking(Quotation quotation, UUID providerId, UUID customerId,
                                          OffsetDateTime startAt, OffsetDateTime endAt) {
        Booking booking = new Booking();
        booking.setQuotationId(quotation.getId());
        booking.setCustomerId(customerId);
        booking.setProviderId(providerId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        bookingRepository.saveAndFlush(booking);
        schedulingService.reserve(providerId, booking.getId(), startAt, endAt);
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, AuthenticatedUser user) {
        Booking booking = require(bookingId);
        if (!isAdmin(user) && !isCustomerOwner(booking, user) && !isProviderOwner(booking, user)) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You are not allowed to view this booking");
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse start(UUID bookingId, AuthenticatedUser user) {
        Booking booking = require(bookingId);
        requireOwningProvider(booking, user);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Only a confirmed booking can be started");
        }
        booking.setStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse complete(UUID bookingId, AuthenticatedUser user) {
        Booking booking = require(bookingId);
        requireOwningProvider(booking, user);
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "Only an in-progress booking can be completed");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        schedulingService.release(booking.getId());
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, AuthenticatedUser user, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "A cancellation reason is required");
        }
        Booking booking = require(bookingId);
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "A booking in status " + status + " cannot be cancelled");
        }
        boolean authorized;
        if (isAdmin(user)) {
            authorized = true;
        } else if (status == BookingStatus.CONFIRMED) {
            authorized = isCustomerOwner(booking, user) || isProviderOwner(booking, user);
        } else {
            // IN_PROGRESS is admin-only per the cancellation actor matrix.
            authorized = false;
        }
        if (!authorized) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You are not allowed to cancel this booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        schedulingService.release(booking.getId());
        auditService.record(user.id(), user.role().name(), "booking.cancel", "booking", booking.getId(),
                Map.of("status", status.name()),
                Map.of("status", BookingStatus.CANCELLED.name()),
                reason);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public boolean hasBookingForQuotation(UUID quotationId) {
        return bookingRepository.findByQuotationId(quotationId).isPresent();
    }

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getQuotationId(),
                booking.getCustomerId(),
                booking.getProviderId(),
                booking.getStatus(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getCreatedAt());
    }

    private Booking require(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Booking not found"));
    }

    private boolean isAdmin(AuthenticatedUser user) {
        return user.role() == UserRole.ADMIN;
    }

    private boolean isCustomerOwner(Booking booking, AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            return false;
        }
        CustomerProfile customer = customerProfileRepository.findByUserAccountId(user.id()).orElse(null);
        return customer != null && customer.getId().equals(booking.getCustomerId());
    }

    private boolean isProviderOwner(Booking booking, AuthenticatedUser user) {
        if (user.role() != UserRole.PROVIDER) {
            return false;
        }
        return providerProfileRepository.findByUserAccountId(user.id())
                .map(provider -> provider.getId().equals(booking.getProviderId()))
                .orElse(false);
    }

    private void requireOwningProvider(Booking booking, AuthenticatedUser user) {
        if (!isProviderOwner(booking, user)) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Only the assigned provider may act on this booking");
        }
    }
}
