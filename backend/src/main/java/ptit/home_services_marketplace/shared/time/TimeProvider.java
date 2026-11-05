package ptit.home_services_marketplace.shared.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Central time source. Persistence is UTC; business/local time uses the
 * canonical {@code Asia/Ho_Chi_Minh} zone.
 */
@Component
public class TimeProvider {

    private final Clock clock;
    private final ZoneId zone;

    public TimeProvider(Clock clock,
                        @Value("${app.time.zone:Asia/Ho_Chi_Minh}") String zoneId) {
        this.clock = clock;
        this.zone = ZoneId.of(zoneId);
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    public ZoneId zone() {
        return zone;
    }

    public ZonedDateTime inLocal(OffsetDateTime instant) {
        return instant.atZoneSameInstant(zone);
    }
}
