package ptit.home_services_marketplace.shared.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clearMdc() {
        LogContext.clear();
    }

    @Test
    void assignsGeneratedTraceIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/service-requests");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] traceIdDuringChain = new String[1];
        FilterChain chain = (req, res) -> traceIdDuringChain[0] = org.slf4j.MDC.get(LogContext.TRACE_ID);

        filter.doFilter(request, response, chain);

        assertThat(traceIdDuringChain[0]).isNotBlank();
        assertThat(response.getHeader(RequestTraceFilter.TRACE_ID_HEADER)).isEqualTo(traceIdDuringChain[0]);
        assertThat(org.slf4j.MDC.get(LogContext.TRACE_ID)).isNull();
    }

    @Test
    void reusesSafeClientTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories");
        request.addHeader(RequestTraceFilter.TRACE_ID_HEADER, "abc-123_XYZ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] traceIdDuringChain = new String[1];
        FilterChain chain = (req, res) -> traceIdDuringChain[0] = org.slf4j.MDC.get(LogContext.TRACE_ID);

        filter.doFilter(request, response, chain);

        assertThat(traceIdDuringChain[0]).isEqualTo("abc-123_XYZ");
        assertThat(response.getHeader(RequestTraceFilter.TRACE_ID_HEADER)).isEqualTo("abc-123_XYZ");
    }

    @Test
    void replacesUnsafeClientTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories");
        request.addHeader(RequestTraceFilter.TRACE_ID_HEADER, "bad value\nwith newline");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        String echoed = response.getHeader(RequestTraceFilter.TRACE_ID_HEADER);
        assertThat(echoed).isNotBlank();
        assertThat(echoed).isNotEqualTo("bad value\nwith newline");
        assertThat(echoed).matches("[A-Za-z0-9._-]{1,64}");
    }
}
