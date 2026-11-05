package ptit.home_services_marketplace.request;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptit.home_services_marketplace.request.dto.ServiceRequestResponse;
import ptit.home_services_marketplace.request.dto.ServiceRequestUpsertRequest;
import ptit.home_services_marketplace.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * Customer service-request endpoints. Only the owning customer may read or act on a request.
 */
@RestController
@RequestMapping("/api/v1")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/service-requests")
    public ServiceRequestResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody ServiceRequestUpsertRequest request) {
        return requestService.createDraft(user, request);
    }

    @PutMapping("/service-requests/{id}")
    public ServiceRequestResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody ServiceRequestUpsertRequest request) {
        return requestService.updateDraft(user, id, request);
    }

    @GetMapping("/service-requests/{id}")
    public ServiceRequestResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID id) {
        return requestService.getById(user, id);
    }

    @GetMapping("/me/requests")
    public List<ServiceRequestResponse> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return requestService.listMyRequests(user);
    }

    @PostMapping("/service-requests/{id}/confirm")
    public ServiceRequestResponse confirm(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable UUID id,
                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return requestService.confirm(user, id, idempotencyKey);
    }

    @PostMapping("/service-requests/{id}/cancel")
    public ServiceRequestResponse cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID id) {
        return requestService.cancel(user, id);
    }
}
