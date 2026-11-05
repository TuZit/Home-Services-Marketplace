package ptit.home_services_marketplace.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "provider_availability")
@Getter
@Setter
@NoArgsConstructor
public class ProviderAvailability {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Convert(converter = LocalTimeMinutesConverter.class)
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Convert(converter = LocalTimeMinutesConverter.class)
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
