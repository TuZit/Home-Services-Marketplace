package ptit.home_services_marketplace.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByQuotationId(UUID quotationId);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Booking> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
