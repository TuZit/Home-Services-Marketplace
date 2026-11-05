package ptit.home_services_marketplace.provider.dto;

import java.time.LocalTime;

/**
 * A single weekly availability window. {@code dayOfWeek} follows the JavaScript
 * {@code Date.getDay()} convention: 0 = Sunday .. 6 = Saturday.
 */
public record AvailabilityWindow(
        java.util.UUID id,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}
