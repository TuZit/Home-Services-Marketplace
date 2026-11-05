package ptit.home_services_marketplace.discovery.dto;

import java.time.LocalTime;

/**
 * A weekly availability window. {@code dayOfWeek} 0 = Sunday .. 6 = Saturday.
 */
public record AvailabilityWindowResponse(
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}
