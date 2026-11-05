package ptit.home_services_marketplace.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "geo_area")
@Getter
@Setter
@NoArgsConstructor
public class GeoArea {

    @Id
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "parent_code", length = 20)
    private String parentCode;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
