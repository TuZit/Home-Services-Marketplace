package ptit.home_services_marketplace.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorId,
        String actorRole,
        String action,
        String entityType,
        UUID entityId,
        String reason,
        OffsetDateTime createdAt) {
}
