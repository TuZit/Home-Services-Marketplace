package ptit.home_services_marketplace.shared.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes immutable {@link AuditEvent} rows for sensitive actions.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID actorId, String actorRole, String action, String entityType,
                       UUID entityId, Object before, Object after, String reason) {
        AuditEvent event = new AuditEvent();
        event.setActorId(actorId);
        event.setActorRole(actorRole);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setBeforeSummary(toJson(before));
        event.setAfterSummary(toJson(after));
        event.setReason(reason);
        repository.save(event);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
