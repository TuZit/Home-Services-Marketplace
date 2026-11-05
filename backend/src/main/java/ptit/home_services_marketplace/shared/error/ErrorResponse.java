package ptit.home_services_marketplace.shared.error;

/**
 * Consistent JSON error body used by the global exception handler.
 */
public record ErrorResponse(String code, String message) {
}
