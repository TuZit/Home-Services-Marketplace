package ptit.home_services_marketplace.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestVersionRepository extends JpaRepository<RequestVersion, UUID> {

    Optional<RequestVersion> findByRequestIdAndVersion(UUID requestId, Integer version);
}
