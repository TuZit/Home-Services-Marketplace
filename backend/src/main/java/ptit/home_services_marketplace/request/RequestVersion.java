package ptit.home_services_marketplace.request;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_version")
@Getter
@Setter
@NoArgsConstructor
public class RequestVersion {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "address_snapshot", nullable = false)
    private String addressSnapshot;

    @Column(name = "area_code", nullable = false, length = 20)
    private String areaCode;

    @Column(name = "preferred_start", nullable = false)
    private OffsetDateTime preferredStart;

    @Column(name = "preferred_end", nullable = false)
    private OffsetDateTime preferredEnd;

    @Column(name = "budget_amount", precision = 14, scale = 0)
    private BigDecimal budgetAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "budget_currency", nullable = false, length = 3)
    private String budgetCurrency = "VND";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
