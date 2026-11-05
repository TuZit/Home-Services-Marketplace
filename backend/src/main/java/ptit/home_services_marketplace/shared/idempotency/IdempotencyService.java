package ptit.home_services_marketplace.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import java.time.Duration;
import java.util.UUID;

/**
 * Persistent idempotency backed by the {@code idempotency_record} table.
 *
 * <p>Callers reserve a key at the start of a consequential command (inside the
 * same transaction), do the work, then complete the record with the response.
 * A replayed request receives the previously stored response body; a different
 * payload under the same key raises a conflict.</p>
 */
@Service
public class IdempotencyService {

    public record Reservation(UUID recordId, boolean replay, int responseCode, String responseBody) {
        public static Reservation proceed(UUID recordId) {
            return new Reservation(recordId, false, 0, null);
        }

        public static Reservation replay(int responseCode, String responseBody) {
            return new Reservation(null, true, responseCode, responseBody);
        }
    }

    private final IdempotencyRecordRepository repository;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository,
                              TimeProvider timeProvider,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Reservation reserve(UUID actorId, String operation, String idemKey, String requestHash) {
        var existing = repository.findByActorIdAndOperationAndIdemKey(actorId, operation, idemKey);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), requestHash);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setActorId(actorId);
        record.setOperation(operation);
        record.setIdemKey(idemKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyRecord.NEW);
        record.setExpiresAt(timeProvider.now().plus(Duration.ofDays(1)));
        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            // Concurrent request won the insert race; re-read and act on it.
            var raced = repository.findByActorIdAndOperationAndIdemKey(actorId, operation, idemKey)
                    .orElseThrow(() -> e);
            return handleExisting(raced, requestHash);
        }
        return Reservation.proceed(record.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID recordId, Object responseBody) {
        IdempotencyRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record missing: " + recordId));
        record.setStatus(IdempotencyRecord.COMPLETED);
        record.setResponseCode(200);
        try {
            record.setResponseBody(objectMapper.writeValueAsString(responseBody));
        } catch (Exception e) {
            record.setResponseBody("{}");
        }
        repository.save(record);
    }

    public <T> T replayBody(Reservation reservation, Class<T> type) {
        try {
            return objectMapper.readValue(reservation.responseBody(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Stored idempotent response could not be replayed", e);
        }
    }

    private Reservation handleExisting(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw ApiException.conflict(ErrorCodes.IDEMPOTENCY_CONFLICT,
                    "Idempotency key was already used with a different request");
        }
        if (IdempotencyRecord.COMPLETED.equals(record.getStatus())) {
            return Reservation.replay(
                    record.getResponseCode() == null ? 200 : record.getResponseCode(),
                    record.getResponseBody());
        }
        throw ApiException.conflict(ErrorCodes.IDEMPOTENCY_CONFLICT,
                "Operation with this idempotency key is still in progress");
    }
}
