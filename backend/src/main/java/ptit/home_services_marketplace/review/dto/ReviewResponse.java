package ptit.home_services_marketplace.review.dto;

import ptit.home_services_marketplace.review.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID customerId,
        UUID providerId,
        Short rating,
        String comment,
        ReviewStatus status,
        OffsetDateTime createdAt) {
}
