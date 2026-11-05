package ptit.home_services_marketplace.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    @Query("select avg(r.rating) from Review r where r.providerId = :providerId and r.status = 'PUBLISHED'")
    Double averagePublishedRating(@Param("providerId") UUID providerId);
}
