package ptit.home_services_marketplace.admin.dto;

import ptit.home_services_marketplace.review.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminReviewResponse(
        UUID id,
        UUID bookingId,
        UUID customerId,
        UUID providerId,
        Short rating,
        String comment,
        ReviewStatus status,
        OffsetDateTime createdAt) {
}
