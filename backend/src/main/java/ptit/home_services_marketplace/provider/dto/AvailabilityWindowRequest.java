package ptit.home_services_marketplace.provider.dto;

import java.time.LocalTime;

/**
 * Create payload for a weekly availability window. {@code dayOfWeek} follows the
 * JavaScript {@code Date.getDay()} convention: 0 = Sunday .. 6 = Saturday.
 */
public record AvailabilityWindowRequest(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}
