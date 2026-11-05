package ptit.home_services_marketplace.customer;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.customer.dto.AddressRequest;
import ptit.home_services_marketplace.customer.dto.AddressResponse;
import ptit.home_services_marketplace.customer.dto.CustomerProfileResponse;
import ptit.home_services_marketplace.customer.dto.UpdateCustomerProfileRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * Self-service customer profile and address book.
 */
@RestController
@RequestMapping("/api/v1/me")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public CustomerProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return customerService.getProfile(user);
    }

    @PutMapping
    public CustomerProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody UpdateCustomerProfileRequest request) {
        return customerService.updateProfile(user, request);
    }

    @GetMapping("/addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal AuthenticatedUser user) {
        return customerService.listAddresses(user);
    }

    @PostMapping("/addresses")
    public AddressResponse createAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody AddressRequest request) {
        return customerService.createAddress(user, request);
    }

    @PutMapping("/addresses/{id}")
    public AddressResponse updateAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody AddressRequest request) {
        return customerService.updateAddress(user, id, request);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id) {
        customerService.deleteAddress(user, id);
        return ResponseEntity.noContent().build();
    }
}
