package ptit.home_services_marketplace.provider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderServiceRepository extends JpaRepository<ProviderService, UUID> {

    List<ProviderService> findByProviderId(UUID providerId);

    List<ProviderService> findByServiceIdAndActiveTrue(UUID serviceId);

    Optional<ProviderService> findByProviderIdAndServiceId(UUID providerId, UUID serviceId);
}
