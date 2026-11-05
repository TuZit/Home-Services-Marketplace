package ptit.home_services_marketplace.provider;

import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.catalog.GeoArea;
import ptit.home_services_marketplace.catalog.GeoAreaRepository;
import ptit.home_services_marketplace.catalog.ServiceRepository;
import ptit.home_services_marketplace.identity.UserRole;
import ptit.home_services_marketplace.provider.dto.AvailabilityWindow;
import ptit.home_services_marketplace.provider.dto.AvailabilityWindowRequest;
import ptit.home_services_marketplace.provider.dto.ProviderProfileResponse;
import ptit.home_services_marketplace.provider.dto.UpdateProviderProfileRequest;
import ptit.home_services_marketplace.shared.audit.AuditService;
import ptit.home_services_marketplace.shared.error.ApiException;
import ptit.home_services_marketplace.shared.error.ErrorCodes;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Provider self-service use cases: profile, offered services, covered areas, weekly
 * availability and submission for verification.
 *
 * <p>Named {@code ProviderManagementService} to avoid colliding with the
 * {@link ProviderService} entity that models the {@code provider_service} table.</p>
 */
@org.springframework.stereotype.Service
public class ProviderManagementService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderAreaRepository providerAreaRepository;
    private final ProviderAvailabilityRepository providerAvailabilityRepository;
    private final ServiceRepository serviceRepository;
    private final GeoAreaRepository geoAreaRepository;
    private final AuditService auditService;

    public ProviderManagementService(ProviderProfileRepository providerProfileRepository,
                                     ProviderServiceRepository providerServiceRepository,
                                     ProviderAreaRepository providerAreaRepository,
                                     ProviderAvailabilityRepository providerAvailabilityRepository,
                                     ServiceRepository serviceRepository,
                                     GeoAreaRepository geoAreaRepository,
                                     AuditService auditService) {
        this.providerProfileRepository = providerProfileRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.providerAreaRepository = providerAreaRepository;
        this.providerAvailabilityRepository = providerAvailabilityRepository;
        this.serviceRepository = serviceRepository;
        this.geoAreaRepository = geoAreaRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------------ profile

    @Transactional(readOnly = true)
    public ProviderProfileResponse getProfile(AuthenticatedUser user) {
        return toProfileResponse(requireProfile(user));
    }

    @Transactional
    public ProviderProfileResponse updateProfile(AuthenticatedUser user, UpdateProviderProfileRequest request) {
        ProviderProfile profile = requireProfile(user);
        if (request.businessName() != null && !request.businessName().isBlank()) {
            profile.setBusinessName(request.businessName().trim());
        }
        if (request.description() != null) {
            profile.setDescription(trimToNull(request.description()));
        }
        if (request.experienceSummary() != null) {
            profile.setExperienceSummary(trimToNull(request.experienceSummary()));
        }
        if (request.contactPhone() != null) {
            profile.setContactPhone(trimToNull(request.contactPhone()));
        }
        providerProfileRepository.save(profile);
        return toProfileResponse(profile);
    }

    // ----------------------------------------------------------------------- services

    @Transactional(readOnly = true)
    public List<UUID> listServiceIds(AuthenticatedUser user) {
        ProviderProfile profile = requireProfile(user);
        return providerServiceRepository.findByProviderId(profile.getId()).stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                .map(ProviderService::getServiceId)
                .toList();
    }

    @Transactional
    public List<UUID> replaceServices(AuthenticatedUser user, List<UUID> serviceIds) {
        ProviderProfile profile = requireProfile(user);
        Set<UUID> requested = new LinkedHashSet<>(serviceIds == null ? List.of() : serviceIds);

        for (UUID serviceId : requested) {
            if (serviceId == null) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION, "serviceIds must not contain null");
            }
            ptit.home_services_marketplace.catalog.Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION,
                            "Unknown service: " + serviceId));
            if (!Boolean.TRUE.equals(service.getActive())) {
                throw ApiException.unprocessable(ErrorCodes.VALIDATION,
                        "Service is not active: " + serviceId);
            }
        }

        List<ProviderService> existing = providerServiceRepository.findByProviderId(profile.getId());
        for (ProviderService ps : existing) {
            boolean shouldBeActive = requested.contains(ps.getServiceId());
            if (shouldBeActive != Boolean.TRUE.equals(ps.getActive())) {
                ps.setActive(shouldBeActive);
                providerServiceRepository.save(ps);
            }
        }
        for (UUID serviceId : requested) {
            if (providerServiceRepository.findByProviderIdAndServiceId(profile.getId(), serviceId).isEmpty()) {
                ProviderService ps = new ProviderService();
                ps.setProviderId(profile.getId());
                ps.setServiceId(serviceId);
                ps.setActive(true);
                providerServiceRepository.save(ps);
            }
        }
        return listServiceIds(user);
    }

    // -------------------------------------------------------------------------- areas

    @Transactional(readOnly = true)
    public List<String> listAreaCodes(AuthenticatedUser user) {
        ProviderProfile profile = requireProfile(user);
        return providerAreaRepository.findByProviderId(profile.getId()).stream()
                .filter(pa -> Boolean.TRUE.equals(pa.getActive()))
                .map(ProviderArea::getAreaCode)
                .toList();
    }

    @Transactional
    public List<String> replaceAreas(AuthenticatedUser user, List<String> areaCodes) {
        ProviderProfile profile = requireProfile(user);
        Set<String> requested = new LinkedHashSet<>();
        if (areaCodes != null) {
            for (String code : areaCodes) {
                requested.add(requireArea(code));
            }
        }

        List<ProviderArea> existing = providerAreaRepository.findByProviderId(profile.getId());
        for (ProviderArea pa : existing) {
            boolean shouldBeActive = requested.contains(pa.getAreaCode());
            if (shouldBeActive != Boolean.TRUE.equals(pa.getActive())) {
                pa.setActive(shouldBeActive);
                providerAreaRepository.save(pa);
            }
        }
        for (String code : requested) {
            if (providerAreaRepository.findByProviderIdAndAreaCode(profile.getId(), code).isEmpty()) {
                ProviderArea pa = new ProviderArea();
                pa.setProviderId(profile.getId());
                pa.setAreaCode(code);
                pa.setActive(true);
                providerAreaRepository.save(pa);
            }
        }
        return listAreaCodes(user);
    }

    // ------------------------------------------------------------------- availability

    @Transactional(readOnly = true)
    public List<AvailabilityWindow> listAvailability(AuthenticatedUser user) {
        ProviderProfile profile = requireProfile(user);
        return providerAvailabilityRepository.findByProviderId(profile.getId()).stream()
                .map(ProviderManagementService::toAvailabilityWindow)
                .toList();
    }

    @Transactional
    public List<AvailabilityWindow> replaceAvailability(AuthenticatedUser user,
                                                        List<AvailabilityWindowRequest> windows) {
        ProviderProfile profile = requireProfile(user);
        List<AvailabilityWindowRequest> incoming = windows == null ? List.of() : windows;

        // Validate everything before mutating so a bad payload leaves state untouched.
        List<ProviderAvailability> toPersist = new ArrayList<>();
        for (AvailabilityWindowRequest window : incoming) {
            if (window == null || window.dayOfWeek() == null || window.startTime() == null
                    || window.endTime() == null) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION,
                        "Each availability window requires dayOfWeek, startTime and endTime");
            }
            short day = window.dayOfWeek();
            if (day < 0 || day > 6) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION, "dayOfWeek must be between 0 and 6");
            }
            LocalTime start = window.startTime();
            LocalTime end = window.endTime();
            if (!start.isBefore(end)) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION, "startTime must be before endTime");
            }
            ProviderAvailability availability = new ProviderAvailability();
            availability.setProviderId(profile.getId());
            availability.setDayOfWeek(day);
            availability.setStartTime(start);
            availability.setEndTime(end);
            toPersist.add(availability);
        }

        providerAvailabilityRepository.deleteByProviderId(profile.getId());
        providerAvailabilityRepository.flush();
        providerAvailabilityRepository.saveAll(toPersist);
        return listAvailability(user);
    }

    // -------------------------------------------------------------- submit verification

    @Transactional
    public ProviderProfileResponse submitVerification(AuthenticatedUser user) {
        ProviderProfile profile = requireProfile(user);
        if (profile.getBusinessName() == null || profile.getBusinessName().isBlank()) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "businessName is required before verification");
        }
        ProviderVerificationStatus status = profile.getVerificationStatus();
        if (status == ProviderVerificationStatus.VERIFIED) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Provider is already verified");
        }
        if (status == ProviderVerificationStatus.PENDING_VERIFICATION) {
            throw ApiException.conflict(ErrorCodes.INVALID_STATE, "Verification is already pending");
        }
        if (status == ProviderVerificationStatus.SUSPENDED) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Provider is suspended");
        }

        boolean hasService = providerServiceRepository.findByProviderId(profile.getId()).stream()
                .anyMatch(ps -> Boolean.TRUE.equals(ps.getActive()));
        boolean hasArea = providerAreaRepository.findByProviderId(profile.getId()).stream()
                .anyMatch(pa -> Boolean.TRUE.equals(pa.getActive()));
        if (!hasService && !hasArea) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION,
                    "Provider must offer at least one service or cover at least one area");
        }

        ProviderProfileResponse before = toProfileResponse(profile);
        profile.setVerificationStatus(ProviderVerificationStatus.PENDING_VERIFICATION);
        providerProfileRepository.save(profile);
        auditService.record(user.id(), user.role().name(), "provider.submit-verification",
                "provider_profile", profile.getId(), before, toProfileResponse(profile), null);
        return toProfileResponse(profile);
    }

    // -------------------------------------------------------------------------- helpers

    private ProviderProfile requireProfile(AuthenticatedUser user) {
        if (user.role() != UserRole.PROVIDER) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Authenticated account is not a provider");
        }
        return providerProfileRepository.findByUserAccountId(user.id())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Provider profile not found"));
    }

    private String requireArea(String areaCode) {
        if (areaCode == null || areaCode.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION, "areaCodes must not contain blank values");
        }
        String trimmed = areaCode.trim();
        GeoArea area = geoAreaRepository.findById(trimmed)
                .orElseThrow(() -> ApiException.unprocessable(ErrorCodes.VALIDATION, "Unknown area code"));
        if (!Boolean.TRUE.equals(area.getActive())) {
            throw ApiException.unprocessable(ErrorCodes.VALIDATION, "Area is not active");
        }
        return area.getCode();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static ProviderProfileResponse toProfileResponse(ProviderProfile profile) {
        return new ProviderProfileResponse(profile.getId(), profile.getBusinessName(),
                profile.getDescription(), profile.getExperienceSummary(), profile.getContactPhone(),
                profile.getVerificationStatus());
    }

    static AvailabilityWindow toAvailabilityWindow(ProviderAvailability availability) {
        return new AvailabilityWindow(availability.getId(), availability.getDayOfWeek(),
                availability.getStartTime(), availability.getEndTime());
    }
}
