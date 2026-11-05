package ptit.home_services_marketplace.review.dto;

import java.util.UUID;

/**
 * Aggregated provider reputation: the average rating of the provider's PUBLISHED reviews.
 * {@code averageRating} is {@code null} when the provider has no published reviews.
 */
public record ProviderReputationResponse(UUID providerId, Double averageRating, long publishedReviewCount) {
}
