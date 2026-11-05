package ptit.home_services_marketplace.quotation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    List<Quotation> findBySelectionIdOrderByCreatedAtDesc(UUID selectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.id = :id")
    Optional<Quotation> findByIdWithLock(@Param("id") UUID id);
}
