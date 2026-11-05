package ptit.home_services_marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.admin.dto.AdminUserResponse;
import ptit.home_services_marketplace.identity.UserAccount;
import ptit.home_services_marketplace.identity.UserAccountRepository;
import ptit.home_services_marketplace.identity.UserAccountStatus;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin user administration: listing and account suspension / reactivation (audited).
 */
@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public AdminUserService(UserAccountRepository userAccountRepository, AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userAccountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserResponse suspend(UUID userId, AuthenticatedUser actor) {
        return changeStatus(userId, actor, UserAccountStatus.SUSPENDED, "user.suspend");
    }

    @Transactional
    public AdminUserResponse reactivate(UUID userId, AuthenticatedUser actor) {
        return changeStatus(userId, actor, UserAccountStatus.ACTIVE, "user.reactivate");
    }

    private AdminUserResponse changeStatus(UUID userId, AuthenticatedUser actor,
                                           UserAccountStatus target, String action) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "User not found"));
        UserAccountStatus before = account.getStatus();
        account.setStatus(target);
        userAccountRepository.save(account);
        auditService.record(actor.id(), actor.role().name(), action, "user_account", account.getId(),
                Map.of("status", before == null ? "UNKNOWN" : before.name()),
                Map.of("status", target.name()),
                null);
        return toResponse(account);
    }

    private AdminUserResponse toResponse(UserAccount account) {
        return new AdminUserResponse(account.getId(), account.getEmail(), account.getRole(), account.getStatus());
    }
}
