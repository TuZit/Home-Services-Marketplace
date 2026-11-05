package ptit.home_services_marketplace.review;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.review.dto.CreateReviewRequest;
import ptit.home_services_marketplace.review.dto.ProviderReputationResponse;
import ptit.home_services_marketplace.review.dto.ReviewResponse;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/bookings/{id}/reviews")
    public ReviewResponse create(@PathVariable UUID id,
                                 @AuthenticationPrincipal AuthenticatedUser user,
                                 @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(id, user, request);
    }

    @GetMapping("/providers/{id}/reputation")
    public ProviderReputationResponse reputation(@PathVariable UUID id) {
        return reviewService.getProviderReputation(id);
    }
}
