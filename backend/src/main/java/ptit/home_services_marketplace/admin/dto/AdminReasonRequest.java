package ptit.home_services_marketplace.admin.dto;

/**
 * Optional body carrying an admin decision reason (required for reject/suspend).
 */
public record AdminReasonRequest(String reason) {
}
