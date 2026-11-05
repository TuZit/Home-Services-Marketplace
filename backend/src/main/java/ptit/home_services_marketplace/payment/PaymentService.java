package ptit.home_services_marketplace.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.booking.Booking;
import ptit.home_services_marketplace.booking.BookingRepository;
import ptit.home_services_marketplace.booking.BookingStatus;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.payment.dto.PaymentResponse;
import ptit.home_services_marketplace.payment.dto.PaymentResultRequest;
import ptit.home_services_marketplace.quotation.Quotation;
import ptit.home_services_marketplace.quotation.QuotationRepository;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.idempotency.IdempotencyService;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Payment attempts for a completed booking and the idempotent simulator result callback.
 *
 * <p>At most one {@code RECORDED} payment per booking is enforced by the partial unique index
 * {@code uq_booking_recorded_payment}; a violation is mapped to
 * {@code PAYMENT_ALREADY_RECORDED}.</p>
 */
@Service
public class PaymentService {

    private static final String RESULT_OPERATION = "payment-result";
    private static final String SIMULATOR_ADAPTER = "SIMULATOR";

    private final PaymentRecordRepository paymentRecordRepository;
    private final BookingRepository bookingRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final IdempotencyService idempotencyService;

    public PaymentService(PaymentRecordRepository paymentRecordRepository,
                          BookingRepository bookingRepository,
                          QuotationRepository quotationRepository,
                          CustomerProfileRepository customerProfileRepository,
                          IdempotencyService idempotencyService) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.bookingRepository = bookingRepository;
        this.quotationRepository = quotationRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PaymentResponse createPayment(UUID bookingId, AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer role required");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Booking not found"));
        CustomerProfile customer = customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer profile not found"));
        if (!booking.getCustomerId().equals(customer.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Only the owning customer may pay for this booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Payment requires a completed booking");
        }
        if (paymentRecordRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.RECORDED)) {
            throw ApiException.conflict(ErrorCodes.PAYMENT_ALREADY_RECORDED,
                    "A recorded payment already exists for this booking");
        }
        if (paymentRecordRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PENDING)) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE,
                    "A pending payment attempt already exists for this booking");
        }
        Quotation quotation = quotationRepository.findById(booking.getQuotationId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Quotation not found"));

        PaymentRecord payment = new PaymentRecord();
        payment.setBookingId(bookingId);
        payment.setAdapter(SIMULATOR_ADAPTER);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(quotation.getTotal());
        payment.setCurrency(quotation.getCurrency());
        paymentRecordRepository.save(payment);
        return toResponse(payment);
    }

    /**
     * Simulator callback: transitions a payment to RECORDED or FAILED. Idempotent on the
     * {@code payment-result} operation.
     */
    @Transactional
    public PaymentResponse recordResult(UUID paymentId, AuthenticatedUser user, String idempotencyKey,
                                        PaymentResultRequest request) {
        if (request == null || (request.status() != PaymentStatus.RECORDED
                && request.status() != PaymentStatus.FAILED)) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "status must be RECORDED or FAILED");
        }
        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? paymentId.toString()
                : idempotencyKey;
        String requestHash = sha256Hex(user.id() + "|" + paymentId + "|" + request.status()
                + "|" + request.externalRef());
        IdempotencyService.Reservation reservation =
                idempotencyService.reserve(user.id(), RESULT_OPERATION, key, requestHash);
        if (reservation.replay()) {
            return idempotencyService.replayBody(reservation, PaymentResponse.class);
        }

        PaymentRecord payment = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Payment not found"));

        PaymentResponse response;
        if (payment.getStatus() == PaymentStatus.RECORDED) {
            // Already recorded: return the existing outcome without a second RECORDED write.
            response = toResponse(payment);
        } else {
            payment.setStatus(request.status());
            if (request.externalRef() != null) {
                payment.setExternalRef(request.externalRef());
            }
            try {
                paymentRecordRepository.saveAndFlush(payment);
            } catch (DataIntegrityViolationException ex) {
                if (paymentRecordRepository.existsByBookingIdAndStatus(payment.getBookingId(), PaymentStatus.RECORDED)) {
                    throw ApiException.conflict(ErrorCodes.PAYMENT_ALREADY_RECORDED,
                            "A recorded payment already exists for this booking");
                }
                throw ApiException.conflict(ErrorCodes.CONFLICT, "Payment result violates a data constraint");
            }
            response = toResponse(payment);
        }
        idempotencyService.complete(reservation.recordId(), response);
        return response;
    }

    private PaymentResponse toResponse(PaymentRecord payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getAdapter(),
                payment.getExternalRef(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt());
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
