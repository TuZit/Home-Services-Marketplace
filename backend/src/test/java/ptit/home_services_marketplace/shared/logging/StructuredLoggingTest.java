package ptit.home_services_marketplace.shared.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ELK contract C1: the {@code logstash} structured format emits one JSON
 * object per line and folds the {@code traceId}, {@code userId} and {@code role} MDC
 * entries into that object.
 */
class StructuredLoggingTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void logstashFormatSerialisesMdcCorrelationFields() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.putObject(Environment.class.getName(), new StandardEnvironment());

        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("logstash");
        encoder.start();

        MDC.put(LogContext.TRACE_ID, "abc123trace");
        MDC.put(LogContext.USER_ID, "user-0001");
        MDC.put(LogContext.ROLE, "CUSTOMER");

        Logger logger = context.getLogger("ptit.home_services_marketplace.access");
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO,
                "GET /api/v1/categories -> 200 (5 ms)", null, null);

        String json = new String(encoder.encode(event), StandardCharsets.UTF_8);

        assertThat(json).contains("\"@timestamp\"");
        assertThat(json).contains("\"traceId\":\"abc123trace\"");
        assertThat(json).contains("\"userId\":\"user-0001\"");
        assertThat(json).contains("\"role\":\"CUSTOMER\"");
        assertThat(json.trim().lines().count()).isEqualTo(1);
    }
}
