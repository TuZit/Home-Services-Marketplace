package ptit.home_services_marketplace.booking.dto;

import ptit.home_services_marketplace.booking.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID quotationId,
        UUID customerId,
        UUID providerId,
        BookingStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime createdAt) {
}
