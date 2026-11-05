package ptit.home_services_marketplace.discovery;

import org.springframework.stereotype.Component;
import ptit.home_services_marketplace.catalog.ServiceRepository;
import ptit.home_services_marketplace.identity.UserAccount;
import ptit.home_services_marketplace.identity.UserAccountRepository;
import ptit.home_services_marketplace.identity.UserAccountStatus;
import ptit.home_services_marketplace.provider.ProviderAreaRepository;
import ptit.home_services_marketplace.provider.ProviderAvailability;
import ptit.home_services_marketplace.provider.ProviderAvailabilityRepository;
import ptit.home_services_marketplace.provider.ProviderProfile;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.provider.ProviderService;
import ptit.home_services_marketplace.provider.ProviderServiceRepository;
import ptit.home_services_marketplace.provider.ProviderVerificationStatus;
import ptit.home_services_marketplace.request.RequestVersion;
import ptit.home_services_marketplace.shared.time.TimeProvider;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic provider eligibility rules, evaluated in a fixed order:
 *
 * <ol>
 *   <li>the provider offers the requested service (active {@code provider_service});</li>
 *   <li>the provider's account is {@code ACTIVE} and the profile is {@code VERIFIED};</li>
 *   <li>the provider serves the request's {@code area_code};</li>
 *   <li>the provider's weekly availability covers the request window
 *       (preferred start day-of-week and start/end time).</li>
 * </ol>
 *
 * <p>Providers are returned sorted by business name so results are stable across runs.</p>
 */
@Component
public class EligibilityEngine {

    public static final String NO_SERVICE_MATCH = "NO_SERVICE_MATCH";
    public static final String NO_VERIFIED_PROVIDER = "NO_VERIFIED_PROVIDER";
    public static final String NO_AREA_COVERAGE = "NO_AREA_COVERAGE";
    public static final String NO_AVAILABILITY = "NO_AVAILABILITY";
    public static final String NO_ELIGIBLE_PROVIDER = "NO_ELIGIBLE_PROVIDER";

    public static final String REASON_SERVICE_MATCH = "SERVICE_MATCH";
    public static final String REASON_PROVIDER_VERIFIED = "PROVIDER_VERIFIED";
    public static final String REASON_AREA_MATCH = "AREA_MATCH";
    public static final String REASON_AVAILABILITY_MATCH = "AVAILABILITY_MATCH";

    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProviderAreaRepository providerAreaRepository;
    private final ProviderAvailabilityRepository providerAvailabilityRepository;
    private final TimeProvider timeProvider;

    public EligibilityEngine(ProviderServiceRepository providerServiceRepository,
                             ProviderProfileRepository providerProfileRepository,
                             UserAccountRepository userAccountRepository,
                             ProviderAreaRepository providerAreaRepository,
                             ProviderAvailabilityRepository providerAvailabilityRepository,
                             TimeProvider timeProvider) {
        this.providerServiceRepository = providerServiceRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.providerAreaRepository = providerAreaRepository;
        this.providerAvailabilityRepository = providerAvailabilityRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * Result of a discovery evaluation: either a non-empty list of eligible candidates or an
     * empty list accompanied by the rule at which candidates were exhausted.
     */
    public record EligibilityOutcome(List<EligibleCandidate> candidates, String noResultReason) {

        public boolean hasResults() {
            return !candidates.isEmpty();
        }
    }

    /**
     * An eligible provider enriched with the rules it satisfied and the matching availability.
     */
    public record EligibleCandidate(UUID providerId,
                                    ProviderProfile profile,
                                    List<String> reasonCodes,
                                    List<ProviderAvailability> matchingAvailability) {
    }

    public EligibilityOutcome evaluate(RequestVersion version) {
        List<ProviderService> offerings =
                providerServiceRepository.findByServiceIdAndActiveTrue(version.getServiceId());
        List<UUID> providerIds = offerings.stream()
                .map(ProviderService::getProviderId)
                .distinct()
                .toList();
        if (providerIds.isEmpty()) {
            return new EligibilityOutcome(List.of(), NO_SERVICE_MATCH);
        }

        short day = dayOfWeek(version.getPreferredStart());
        LocalTime windowStart = timeProvider.inLocal(version.getPreferredStart()).toLocalTime();
        LocalTime windowEnd = timeProvider.inLocal(version.getPreferredEnd()).toLocalTime();

        List<EligibleCandidate> eligible = new ArrayList<>();
        boolean anyVerified = false;
        boolean anyArea = false;
        boolean anyAvailability = false;

        for (UUID providerId : providerIds) {
            Optional<ProviderProfile> profile = verifiedActiveProfile(providerId);
            if (profile.isEmpty()) {
                continue;
            }
            anyVerified = true;

            if (!providerAreaRepository.existsByProviderIdAndAreaCodeAndActiveTrue(providerId, version.getAreaCode())) {
                continue;
            }
            anyArea = true;

            List<ProviderAvailability> matching =
                    matchingAvailability(providerId, day, windowStart, windowEnd);
            if (matching.isEmpty()) {
                continue;
            }
            anyAvailability = true;

            eligible.add(new EligibleCandidate(providerId, profile.get(), defaultReasonCodes(), matching));
        }

        if (eligible.isEmpty()) {
            String reason;
            if (!anyVerified) {
                reason = NO_VERIFIED_PROVIDER;
            } else if (!anyArea) {
                reason = NO_AREA_COVERAGE;
            } else if (!anyAvailability) {
                reason = NO_AVAILABILITY;
            } else {
                reason = NO_ELIGIBLE_PROVIDER;
            }
            return new EligibilityOutcome(List.of(), reason);
        }
        return new EligibilityOutcome(eligible, null);
    }

    /**
     * Re-evaluates a single provider against all rules (used to revalidate a selection).
     */
    public Optional<EligibleCandidate> evaluateProvider(RequestVersion version, UUID providerId) {
        Optional<ProviderProfile> profile = verifiedActiveProfile(providerId);
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        if (!providerAreaRepository.existsByProviderIdAndAreaCodeAndActiveTrue(providerId, version.getAreaCode())) {
            return Optional.empty();
        }
        short day = dayOfWeek(version.getPreferredStart());
        LocalTime windowStart = timeProvider.inLocal(version.getPreferredStart()).toLocalTime();
        LocalTime windowEnd = timeProvider.inLocal(version.getPreferredEnd()).toLocalTime();
        List<ProviderAvailability> matching = matchingAvailability(providerId, day, windowStart, windowEnd);
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EligibleCandidate(providerId, profile.get(), defaultReasonCodes(), matching));
    }

    private Optional<ProviderProfile> verifiedActiveProfile(UUID providerId) {
        Optional<ProviderProfile> profile = providerProfileRepository.findById(providerId);
        if (profile.isEmpty() || profile.get().getVerificationStatus() != ProviderVerificationStatus.VERIFIED) {
            return Optional.empty();
        }
        UserAccount account = userAccountRepository.findById(profile.get().getUserAccountId()).orElse(null);
        if (account == null || account.getStatus() != UserAccountStatus.ACTIVE) {
            return Optional.empty();
        }
        return profile;
    }

    private List<ProviderAvailability> matchingAvailability(UUID providerId, short day,
                                                            LocalTime windowStart, LocalTime windowEnd) {
        return providerAvailabilityRepository.findByProviderId(providerId).stream()
                .filter(a -> a.getDayOfWeek() != null && a.getDayOfWeek() == day)
                .filter(a -> !a.getStartTime().isAfter(windowStart) && !a.getEndTime().isBefore(windowEnd))
                .toList();
    }

    private short dayOfWeek(java.time.OffsetDateTime instant) {
        // ISO 1..7 (Mon..Sun) mapped to 0..6 (Sun..Sat) to fit the day_of_week CHECK (0..6).
        int iso = timeProvider.inLocal(instant).getDayOfWeek().getValue();
        return (short) (iso % 7);
    }

    private static List<String> defaultReasonCodes() {
        return List.of(REASON_SERVICE_MATCH, REASON_PROVIDER_VERIFIED, REASON_AREA_MATCH, REASON_AVAILABILITY_MATCH);
    }
}
