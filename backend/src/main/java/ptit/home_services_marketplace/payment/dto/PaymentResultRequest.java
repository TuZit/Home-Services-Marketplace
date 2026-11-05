package ptit.home_services_marketplace.payment.dto;

import ptit.home_services_marketplace.payment.PaymentStatus;

/**
 * Body for the payment simulator callback {@code POST /api/v1/payments/{id}/result}.
 * {@code status} must be {@code RECORDED} or {@code FAILED}.
 */
public record PaymentResultRequest(PaymentStatus status, String externalRef) {
}
