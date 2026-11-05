package ptit.home_services_marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.home_services_marketplace.admin.dto.DashboardResponse;
import ptit.home_services_marketplace.booking.BookingRepository;
import ptit.home_services_marketplace.booking.BookingStatus;
import ptit.home_services_marketplace.provider.ProviderProfileRepository;
import ptit.home_services_marketplace.provider.ProviderVerificationStatus;
import ptit.home_services_marketplace.request.ServiceRequestRepository;

/**
 * Aggregated operational counts for the admin dashboard.
 */
@Service
public class DashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerProfileRepository;

    public DashboardService(ServiceRequestRepository serviceRequestRepository,
                            BookingRepository bookingRepository,
                            ProviderProfileRepository providerProfileRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.bookingRepository = bookingRepository;
        this.providerProfileRepository = providerProfileRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        long requestCount = serviceRequestRepository.count();
        long bookingCount = bookingRepository.count();
        long completedBookingCount = bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED)
                .count();
        long verifiedProviderCount = providerProfileRepository.findAll().stream()
                .filter(provider -> provider.getVerificationStatus() == ProviderVerificationStatus.VERIFIED)
                .count();
        return new DashboardResponse(requestCount, bookingCount, completedBookingCount, verifiedProviderCount);
    }
}
