package ptit.home_services_marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.admin.dto.AdminReviewResponse;
import ptit.home_services_marketplace.review.Review;
import ptit.home_services_marketplace.review.ReviewRepository;
import ptit.home_services_marketplace.review.ReviewStatus;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin moderation of reviews: listing plus hide / restore (audited).
 */
@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final AuditService auditService;

    public AdminReviewService(ReviewRepository reviewRepository, AuditService auditService) {
        this.reviewRepository = reviewRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> list() {
        return reviewRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminReviewResponse hide(UUID reviewId, AuthenticatedUser actor) {
        return changeStatus(reviewId, actor, ReviewStatus.HIDDEN, "review.hide");
    }

    @Transactional
    public AdminReviewResponse restore(UUID reviewId, AuthenticatedUser actor) {
        return changeStatus(reviewId, actor, ReviewStatus.PUBLISHED, "review.restore");
    }

    private AdminReviewResponse changeStatus(UUID reviewId, AuthenticatedUser actor,
                                             ReviewStatus target, String action) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Review not found"));
        ReviewStatus before = review.getStatus();
        review.setStatus(target);
        reviewRepository.save(review);
        auditService.record(actor.id(), actor.role().name(), action, "review", review.getId(),
                Map.of("status", before == null ? "UNKNOWN" : before.name()),
                Map.of("status", target.name()),
                null);
        return toResponse(review);
    }

    private AdminReviewResponse toResponse(Review review) {
        return new AdminReviewResponse(
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
