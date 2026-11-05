package ptit.home_services_marketplace.discovery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "discovery_run")
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryRun {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_version", nullable = false)
    private Integer requestVersion;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private OffsetDateTime executedAt;

    @Column(name = "no_result_reason", length = 40)
    private String noResultReason;
}
