package ptit.home_services_marketplace.customer.dto;

/**
 * Update payload for the authenticated customer's profile.
 */
public record UpdateCustomerProfileRequest(String fullName, String phone) {
}
