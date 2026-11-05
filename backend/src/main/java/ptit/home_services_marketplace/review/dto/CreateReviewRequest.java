package ptit.home_services_marketplace.review.dto;

/**
 * Body for {@code POST /api/v1/bookings/{id}/reviews}.
 */
public record CreateReviewRequest(Integer rating, String comment) {
}
