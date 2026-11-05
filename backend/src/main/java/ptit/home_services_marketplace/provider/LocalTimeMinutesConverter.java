package ptit.home_services_marketplace.provider;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;

/**
 * Maps {@link LocalTime} to minutes-from-midnight and back.
 *
 * <p>Persisting {@code LocalTime} to a PostgreSQL {@code time} column through the JDBC
 * driver applies a JVM-default-timezone shift (midnight-relative {@code java.sql.Time}
 * round-tripping). Storing an integer minute-of-day keeps weekly availability stable and
 * timezone independent.</p>
 */
@Converter
public class LocalTimeMinutesConverter implements AttributeConverter<LocalTime, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LocalTime attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getHour() * 60 + attribute.getMinute();
    }

    @Override
    public LocalTime convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return LocalTime.of(dbData / 60, dbData % 60);
    }
}
