package ptit.home_services_marketplace.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlation and access logging for every HTTP request.
 *
 * <p>Each request is given a {@code traceId} that is stored in the MDC under
 * {@link LogContext#TRACE_ID} so it appears on every log line written while the request is
 * in flight, echoed back in the {@code X-Request-Id} response header, and reported in a
 * single access-log line with method, path, status and duration.</p>
 *
 * <p>The access line also carries structured {@code http_method}, {@code http_status} and
 * {@code http_duration_ms} members (via the SLF4J fluent API) so the ELK stack can
 * aggregate status and latency without parsing the message text. The request path/query is
 * sanitised (control characters stripped, length capped) to prevent log injection.</p>
 *
 * <p>Declared with {@link Ordered#HIGHEST_PRECEDENCE} so it wraps the Spring Security
 * filter chain: requests rejected with 401/403 are still correlated and logged.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Request-Id";

    private static final Logger accessLog =
            LoggerFactory.getLogger("ptit.home_services_marketplace.access");
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern UNSAFE_LOG_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final int MAX_LOGGED_URI_LENGTH = 512;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        LogContext.setTraceId(traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();
            String uri = sanitize(requestUri(request));
            accessLog.atInfo()
                    .addKeyValue("http_method", request.getMethod())
                    .addKeyValue("http_status", status)
                    .addKeyValue("http_duration_ms", durationMs)
                    .log("{} {} -> {} ({} ms)", request.getMethod(), uri, status, durationMs);
            LogContext.clear();
        }
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header != null && SAFE_TRACE_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String requestUri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private static String sanitize(String value) {
        String cleaned = UNSAFE_LOG_CHARS.matcher(value).replaceAll("");
        return cleaned.length() > MAX_LOGGED_URI_LENGTH
                ? cleaned.substring(0, MAX_LOGGED_URI_LENGTH)
                : cleaned;
    }
}
