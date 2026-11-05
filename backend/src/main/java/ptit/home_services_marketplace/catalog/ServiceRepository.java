package ptit.home_services_marketplace.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByCategoryIdAndActiveTrueOrderByNameAsc(UUID categoryId);

    Optional<Service> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
