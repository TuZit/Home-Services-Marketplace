package ptit.home_services_marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.admin.dto.AdminProviderResponse;
import ptit.home_services_marketplace.provider.ProviderProfile;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.provider.ProviderVerificationStatus;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.Map;
import java.util.UUID;

/**
 * Admin verification decisions over provider profiles. Every decision is audited.
 */
@Service
public class AdminProviderService {

    private final ProviderProfileRepository providerProfileRepository;
    private final AuditService auditService;

    public AdminProviderService(ProviderProfileRepository providerProfileRepository,
                                AuditService auditService) {
        this.providerProfileRepository = providerProfileRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AdminProviderResponse verify(UUID providerId, AuthenticatedUser actor) {
        return decide(providerId, actor, ProviderVerificationStatus.VERIFIED, null, "provider.verify");
    }

    @Transactional
    public AdminProviderResponse reject(UUID providerId, AuthenticatedUser actor, String reason) {
        return decide(providerId, actor, ProviderVerificationStatus.REJECTED, reason, "provider.reject");
    }

    @Transactional
    public AdminProviderResponse suspend(UUID providerId, AuthenticatedUser actor, String reason) {
        return decide(providerId, actor, ProviderVerificationStatus.SUSPENDED, reason, "provider.suspend");
    }

    private AdminProviderResponse decide(UUID providerId, AuthenticatedUser actor,
                                         ProviderVerificationStatus target, String reason, String action) {
        ProviderProfile provider = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Provider not found"));
        if ((target == ProviderVerificationStatus.REJECTED || target == ProviderVerificationStatus.SUSPENDED)
                && (reason == null || reason.isBlank())) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "A reason is required");
        }
        ProviderVerificationStatus before = provider.getVerificationStatus();
        provider.setVerificationStatus(target);
        providerProfileRepository.save(provider);
        auditService.record(actor.id(), actor.role().name(), action, "provider_profile", provider.getId(),
                Map.of("verificationStatus", before == null ? "UNKNOWN" : before.name()),
                Map.of("verificationStatus", target.name()),
                reason);
        return toResponse(provider);
    }

    private AdminProviderResponse toResponse(ProviderProfile provider) {
        return new AdminProviderResponse(
                provider.getId(),
                provider.getUserAccountId(),
                provider.getBusinessName(),
                provider.getVerificationStatus());
    }
}
