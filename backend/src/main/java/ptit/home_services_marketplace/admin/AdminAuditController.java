package ptit.home_services_marketplace.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.admin.dto.AuditEventResponse;
import ptit.home_services_marketplace.shared.audit.AuditEvent;
import ptit.home_services_marketplace.shared.audit.AuditEventRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final AuditEventRepository auditEventRepository;

    public AdminAuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public List<AuditEventResponse> list() {
        return auditEventRepository.findAll().stream().map(AdminAuditController::toResponse).toList();
    }

    private static AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorId(),
                event.getActorRole(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getReason(),
                event.getCreatedAt());
    }
}
