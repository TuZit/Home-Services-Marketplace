package ptit.home_services_marketplace.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderSlotReservationRepository extends JpaRepository<ProviderSlotReservation, UUID> {

    Optional<ProviderSlotReservation> findByBookingId(UUID bookingId);
}
