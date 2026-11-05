package ptit.home_services_marketplace.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    List<PaymentRecord> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    boolean existsByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    Optional<PaymentRecord> findFirstByBookingIdAndStatus(UUID bookingId, PaymentStatus status);
}
