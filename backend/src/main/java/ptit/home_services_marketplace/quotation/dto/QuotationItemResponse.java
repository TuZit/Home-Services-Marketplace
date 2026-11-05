package ptit.home_services_marketplace.quotation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationItemResponse(
        UUID id,
        String description,
        BigDecimal amount,
        String currency) {
}
