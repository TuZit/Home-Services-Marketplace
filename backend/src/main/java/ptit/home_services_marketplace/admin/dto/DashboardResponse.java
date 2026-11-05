package ptit.home_services_marketplace.admin.dto;

/**
 * Operational counts surfaced on {@code GET /api/v1/admin/dashboard}.
 */
public record DashboardResponse(
        long requestCount,
        long bookingCount,
        long completedBookingCount,
        long verifiedProviderCount) {
}
