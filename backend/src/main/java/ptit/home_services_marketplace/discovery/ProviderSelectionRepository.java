package ptit.home_services_marketplace.discovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderSelectionRepository extends JpaRepository<ProviderSelection, UUID> {

    Optional<ProviderSelection> findByRequestIdAndRequestVersion(UUID requestId, Integer requestVersion);

    Optional<ProviderSelection> findByRequestIdAndStatus(UUID requestId, ProviderSelectionStatus status);
}
