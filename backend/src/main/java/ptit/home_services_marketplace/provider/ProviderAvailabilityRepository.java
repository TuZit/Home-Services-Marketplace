package ptit.home_services_marketplace.provider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, UUID> {

    List<ProviderAvailability> findByProviderId(UUID providerId);

    void deleteByProviderId(UUID providerId);
}
