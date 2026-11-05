package ptit.home_services_marketplace.booking;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.booking.dto.BookingResponse;
import ptit.home_services_marketplace.booking.dto.CancelBookingRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.getBooking(id, user);
    }

    @PostMapping("/{id}/start")
    public BookingResponse start(@PathVariable UUID id,
                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.start(id, user);
    }

    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.complete(id, user);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestBody CancelBookingRequest request) {
        return bookingService.cancel(id, user, request == null ? null : request.reason());
    }
}
