package ptit.home_services_marketplace.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
