package ptit.home_services_marketplace.payment.dto;

import ptit.home_services_marketplace.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        String adapter,
        String externalRef,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        OffsetDateTime createdAt) {
}
