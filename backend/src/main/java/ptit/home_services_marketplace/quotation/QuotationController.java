package ptit.home_services_marketplace.quotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.booking.dto.BookingResponse;
import ptit.home_services_marketplace.quotation.dto.CreateQuotationRequest;
import ptit.home_services_marketplace.quotation.dto.QuotationResponse;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

/**
 * Quotation endpoints. The create endpoint lives under {@code /provider} so it is covered by
 * the {@code PROVIDER}-only security matcher; the state transitions are authorized in the
 * service.
 */
@RestController
@RequestMapping("/api/v1")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping("/provider/requests/{id}/quotations")
    public QuotationResponse create(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestBody CreateQuotationRequest request) {
        return quotationService.createDraft(id, user, request);
    }

    @PostMapping("/quotations/{id}/submit")
    public QuotationResponse submit(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return quotationService.submit(id, user);
    }

    @PostMapping("/quotations/{id}/withdraw")
    public QuotationResponse withdraw(@PathVariable UUID id,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return quotationService.withdraw(id, user);
    }

    @PostMapping("/quotations/{id}/accept")
    public BookingResponse accept(@PathVariable UUID id,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return quotationService.accept(id, user, idempotencyKey);
    }

    @PostMapping("/quotations/{id}/reject")
    public QuotationResponse reject(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return quotationService.reject(id, user);
    }

    @GetMapping("/quotations/{id}")
    public QuotationResponse get(@PathVariable UUID id,
                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return quotationService.get(id, user);
    }
}
