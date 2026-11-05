package ptit.home_services_marketplace.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeoAreaRepository extends JpaRepository<GeoArea, String> {

    List<GeoArea> findByActiveTrueOrderByCodeAsc();
}
