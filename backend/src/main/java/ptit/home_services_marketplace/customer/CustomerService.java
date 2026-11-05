package ptit.home_services_marketplace.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.catalog.GeoArea;
import ptit.home_services_marketplace.catalog.GeoAreaRepository;
import ptit.home_services_marketplace.customer.dto.AddressRequest;
import ptit.home_services_marketplace.customer.dto.AddressResponse;
import ptit.home_services_marketplace.customer.dto.CustomerProfileResponse;
import ptit.home_services_marketplace.customer.dto.UpdateCustomerProfileRequest;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import java.util.List;
import java.util.UUID;

/**
 * Self-service customer use cases. The {@link CustomerProfile} is created at registration,
 * so it is always resolved from the authenticated user id rather than supplied by the client.
 */
@Service
public class CustomerService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final GeoAreaRepository geoAreaRepository;
    private final TimeProvider timeProvider;

    public CustomerService(CustomerProfileRepository customerProfileRepository,
                           CustomerAddressRepository customerAddressRepository,
                           GeoAreaRepository geoAreaRepository,
                           TimeProvider timeProvider) {
        this.customerProfileRepository = customerProfileRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.geoAreaRepository = geoAreaRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(AuthenticatedUser user) {
        CustomerProfile profile = requireProfile(user);
        return toProfileResponse(profile);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(AuthenticatedUser user, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = requireProfile(user);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            profile.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            profile.setPhone(trimToNull(request.phone()));
        }
        customerProfileRepository.save(profile);
        return toProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(AuthenticatedUser user) {
        CustomerProfile profile = requireProfile(user);
        return customerAddressRepository.findByCustomerIdAndDeletedAtIsNullOrderByIsDefaultDesc(profile.getId())
                .stream()
                .map(CustomerService::toAddressResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(AuthenticatedUser user, AddressRequest request) {
        CustomerProfile profile = requireProfile(user);
        String label = requireText(request.label(), "label");
        String addressLine = requireText(request.addressLine(), "addressLine");
        String areaCode = requireArea(request.areaCode());

        List<CustomerAddress> existing =
                customerAddressRepository.findByCustomerIdAndDeletedAtIsNullOrderByIsDefaultDesc(profile.getId());
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || existing.isEmpty();

        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(profile.getId());
        address.setLabel(label);
        address.setAddressLine(addressLine);
        address.setAreaCode(areaCode);
        address.setIsDefault(makeDefault);
        if (makeDefault) {
            clearDefaults(existing);
        }
        customerAddressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(AuthenticatedUser user, UUID addressId, AddressRequest request) {
        CustomerProfile profile = requireProfile(user);
        CustomerAddress address = requireAddress(profile, addressId);

        if (request.label() != null) {
            address.setLabel(requireText(request.label(), "label"));
        }
        if (request.addressLine() != null) {
            address.setAddressLine(requireText(request.addressLine(), "addressLine"));
        }
        if (request.areaCode() != null) {
            address.setAreaCode(requireArea(request.areaCode()));
        }
        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                clearDefaults(customerAddressRepository
                        .findByCustomerIdAndDeletedAtIsNullOrderByIsDefaultDesc(profile.getId()));
            }
            address.setIsDefault(request.isDefault());
        }
        customerAddressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(AuthenticatedUser user, UUID addressId) {
        CustomerProfile profile = requireProfile(user);
        CustomerAddress address = requireAddress(profile, addressId);
        address.setDeletedAt(timeProvider.now());
        address.setIsDefault(false);
        customerAddressRepository.save(address);
    }

    // ---------------------------------------------------------------------- helpers

    private CustomerProfile requireProfile(AuthenticatedUser user) {
        if (user.role() != UserRole.CUSTOMER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Authenticated account is not a customer");
        }
        return customerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Customer profile not found"));
    }

    private CustomerAddress requireAddress(CustomerProfile profile, UUID addressId) {
        CustomerAddress address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Address not found"));
        if (!address.getCustomerId().equals(profile.getId()) || address.getDeletedAt() != null) {
            throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Address not found");
        }
        return address;
    }

    private void clearDefaults(List<CustomerAddress> addresses) {
        for (CustomerAddress existing : addresses) {
            if (Boolean.TRUE.equals(existing.getIsDefault())) {
                existing.setIsDefault(false);
                customerAddressRepository.save(existing);
            }
        }
    }

    private String requireArea(String areaCode) {
        if (areaCode == null || areaCode.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "areaCode is required");
        }
        String trimmed = areaCode.trim();
        GeoArea area = geoAreaRepository.findById(trimmed)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown area code"));
        if (!Boolean.TRUE.equals(area.getActive())) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Area is not active");
        }
        return area.getCode();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static CustomerProfileResponse toProfileResponse(CustomerProfile profile) {
        return new CustomerProfileResponse(profile.getId(), profile.getFullName(), profile.getPhone());
    }

    static AddressResponse toAddressResponse(CustomerAddress address) {
        return new AddressResponse(address.getId(), address.getLabel(), address.getAddressLine(),
                address.getAreaCode(), Boolean.TRUE.equals(address.getIsDefault()));
    }
}
