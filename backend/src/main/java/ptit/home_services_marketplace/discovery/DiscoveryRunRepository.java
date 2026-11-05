package ptit.home_services_marketplace.discovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscoveryRunRepository extends JpaRepository<DiscoveryRun, UUID> {
}
