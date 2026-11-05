package ptit.home_services_marketplace.payment;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.payment.dto.PaymentResponse;
import ptit.home_services_marketplace.payment.dto.PaymentResultRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/bookings/{id}/payments")
    public PaymentResponse create(@PathVariable UUID id,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return paymentService.createPayment(id, user);
    }

    @PostMapping("/payments/{id}/result")
    public PaymentResponse result(@PathVariable UUID id,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                  @RequestBody PaymentResultRequest request) {
        return paymentService.recordResult(id, user, idempotencyKey, request);
    }
}
