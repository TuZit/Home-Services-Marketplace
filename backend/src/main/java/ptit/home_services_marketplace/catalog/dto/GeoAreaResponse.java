package ptit.home_services_marketplace.catalog.dto;

/**
 * Public geo area (province or district). Used by the address book so a customer can pick
 * an {@code areaCode} instead of typing one.
 */
public record GeoAreaResponse(String code, String name, String type, String parentCode) {
}
