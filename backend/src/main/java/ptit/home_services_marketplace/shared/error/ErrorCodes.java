package ptit.home_services_marketplace.shared.error;

/**
 * Stable machine-readable error codes returned to clients.
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String VALIDATION = "VALIDATION";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INTERNAL = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String MALFORMED = "MALFORMED_REQUEST";

    public static final String SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE";
    public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String EXPIRED = "EXPIRED";

    public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";

    public static final String NOT_ELIGIBLE = "PROVIDER_NOT_ELIGIBLE";
    public static final String ALREADY_SELECTED = "ALREADY_SELECTED";
    public static final String PAYMENT_ALREADY_RECORDED = "PAYMENT_ALREADY_RECORDED";
    public static final String REVIEW_EXISTS = "REVIEW_EXISTS";
    public static final String REVIEW_NOT_ALLOWED = "REVIEW_NOT_ALLOWED";
}
