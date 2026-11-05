package ptit.home_services_marketplace.shared.logging;

import org.slf4j.MDC;

/**
 * Central registry of MDC keys used for request correlation and a thin helper to mutate
 * them. Keeping the keys here lets the trace filter and the authentication filter agree on
 * a single vocabulary without depending on each other.
 *
 * <p>Values placed here surface in every log line via the configured pattern, so only
 * non-sensitive identifiers (trace id, user id, role) may be stored.</p>
 */
public final class LogContext {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String ROLE = "role";

    private LogContext() {
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void setActor(String userId, String role) {
        MDC.put(USER_ID, userId);
        MDC.put(ROLE, role);
    }

    public static void clear() {
        MDC.clear();
    }
}
