package ptit.home_services_marketplace.quotation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Body for {@code POST /api/v1/provider/requests/{id}/quotations}.
 */
public record CreateQuotationRequest(
        List<Item> items,
        BigDecimal total,
        OffsetDateTime serviceStart,
        OffsetDateTime serviceEnd,
        OffsetDateTime validUntil) {

    public record Item(String description, BigDecimal amount) {
    }
}
