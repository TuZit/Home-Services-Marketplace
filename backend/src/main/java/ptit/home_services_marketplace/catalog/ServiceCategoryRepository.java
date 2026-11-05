package ptit.home_services_marketplace.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    List<ServiceCategory> findByActiveTrueOrderByNameAsc();

    Optional<ServiceCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
