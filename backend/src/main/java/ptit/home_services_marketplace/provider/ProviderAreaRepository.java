package ptit.home_services_marketplace.provider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderAreaRepository extends JpaRepository<ProviderArea, UUID> {

    List<ProviderArea> findByProviderId(UUID providerId);

    Optional<ProviderArea> findByProviderIdAndAreaCode(UUID providerId, String areaCode);

    boolean existsByProviderIdAndAreaCodeAndActiveTrue(UUID providerId, String areaCode);
}
