package ptit.home_services_marketplace.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailNorm(String emailNorm);

    boolean existsByEmailNorm(String emailNorm);
}
