package ptit.home_services_marketplace.scheduling;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Provider slot reservation lifecycle.
 *
 * <p>Concurrency is enforced by the PostgreSQL exclusion constraint
 * {@code ex_provider_active_slot} (gist on {@code provider_id} + time range, restricted to
 * {@code status = 'ACTIVE'}). A colliding insert surfaces as a
 * {@link DataIntegrityViolationException} carrying SQLState {@code 23P01}; it is mapped to a
 * {@code SLOT_UNAVAILABLE} conflict.</p>
 */
@Service
public class SchedulingService {

    private final ProviderSlotReservationRepository reservationRepository;

    public SchedulingService(ProviderSlotReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Reserves an ACTIVE slot for a booking. Flushes immediately so the exclusion constraint
     * is evaluated within the caller's transaction (and rolls back with it on conflict).
     */
    @Transactional
    public ProviderSlotReservation reserve(UUID providerId, UUID bookingId,
                                           OffsetDateTime startAt, OffsetDateTime endAt) {
        ProviderSlotReservation reservation = new ProviderSlotReservation();
        reservation.setProviderId(providerId);
        reservation.setBookingId(bookingId);
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setStatus(SlotReservationStatus.ACTIVE);
        try {
            return reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            if (isExclusionViolation(ex)) {
                throw ApiException.conflict(ErrorCodes.SLOT_UNAVAILABLE,
                        "The provider is already booked for an overlapping time slot");
            }
            throw ApiException.conflict(ErrorCodes.SLOT_UNAVAILABLE,
                    "The provider slot is no longer available");
        }
    }

    /**
     * Releases the ACTIVE slot reservation for a booking. Idempotent: an already released or
     * absent reservation is left untouched.
     */
    @Transactional
    public void release(UUID bookingId) {
        reservationRepository.findByBookingId(bookingId).ifPresent(reservation -> {
            if (reservation.getStatus() == SlotReservationStatus.ACTIVE) {
                reservation.setStatus(SlotReservationStatus.RELEASED);
                reservationRepository.save(reservation);
            }
        });
    }

    private boolean isExclusionViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && "23P01".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
