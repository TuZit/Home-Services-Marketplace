package ptit.home_services_marketplace.booking.dto;

/**
 * Body for {@code POST /api/v1/bookings/{id}/cancel}. A reason is mandatory.
 */
public record CancelBookingRequest(String reason) {
}
