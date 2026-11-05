package ptit.home_services_marketplace.review;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.booking.Booking;
import ptit.home_services_marketplace.booking.BookingRepository;
import ptit.home_services_marketplace.booking.BookingStatus;
import ptit.home_services_marketplace.customer.CustomerProfile;
import ptit.home_services_marketplace.customer.CustomerProfileRepository;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.payment.PaymentRecordRepository;
import ptit.home_services_marketplace.payment.PaymentStatus;
import ptit.home_services_marketplace.review.dto.CreateReviewRequest;
import ptit.home_services_marketplace.review.dto.ProviderReputationResponse;
import ptit.home_services_marketplace.review.dto.ReviewResponse;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

/**
 * Customer reviews for completed, paid bookings and aggregation of provider reputation.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         CustomerProfileRepository customerProfileRepository,
                         PaymentRecordRepository paymentRecordRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Transactional
    public ReviewResponse createReview(UUID bookingId, AuthenticatedUser user, CreateReviewRequest request) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer role required");
        }
        if (request == null || request.rating() == null
                || request.rating() < 1 || request.rating() > 5) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "rating must be between 1 and 5");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Booking not found"));
        CustomerProfile customer = customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.FORBIDDEN, "Customer profile not found"));
        if (!booking.getCustomerId().equals(customer.getId())) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Only the owning customer may review this booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict(ErrorCodes.REVIEW_NOT_ALLOWED, "Only a completed booking can be reviewed");
        }
        if (!paymentRecordRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.RECORDED)) {
            throw ApiException.conflict(ErrorCodes.REVIEW_NOT_ALLOWED,
                    "A recorded payment is required before reviewing");
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw ApiException.conflict(ErrorCodes.REVIEW_EXISTS, "A review already exists for this booking");
        }

        Review review = new Review();
        review.setBookingId(bookingId);
        review.setCustomerId(customer.getId());
        review.setProviderId(booking.getProviderId());
        review.setRating(request.rating().shortValue());
        review.setComment(request.comment());
        review.setStatus(ReviewStatus.PUBLISHED);
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCodes.REVIEW_EXISTS, "A review already exists for this booking");
        }
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public ProviderReputationResponse getProviderReputation(UUID providerId) {
        Double average = reviewRepository.averagePublishedRating(providerId);
        long count = reviewRepository.findAll().stream()
                .filter(review -> review.getProviderId().equals(providerId))
                .filter(review -> review.getStatus() == ReviewStatus.PUBLISHED)
                .count();
        return new ProviderReputationResponse(providerId, average, count);
    }

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBookingId(),
                review.getCustomerId(),
                review.getProviderId(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt());
    }
}
