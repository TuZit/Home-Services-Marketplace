package ptit.home_services_marketplace.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdAndDeletedAtIsNullOrderByIsDefaultDesc(UUID customerId);
}
