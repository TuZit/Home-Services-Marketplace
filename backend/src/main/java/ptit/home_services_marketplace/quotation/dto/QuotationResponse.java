package ptit.home_services_marketplace.quotation.dto;

import ptit.home_services_marketplace.quotation.QuotationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        UUID selectionId,
        QuotationStatus status,
        BigDecimal total,
        String currency,
        OffsetDateTime serviceStart,
        OffsetDateTime serviceEnd,
        OffsetDateTime validUntil,
        OffsetDateTime createdAt,
        List<QuotationItemResponse> items) {
}
