package ptit.home_services_marketplace.discovery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "provider_selection")
@Getter
@Setter
@NoArgsConstructor
public class ProviderSelection {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_version", nullable = false)
    private Integer requestVersion;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "selected_by", nullable = false)
    private UUID selectedBy;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private OffsetDateTime selectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProviderSelectionStatus status = ProviderSelectionStatus.ACTIVE;
}
