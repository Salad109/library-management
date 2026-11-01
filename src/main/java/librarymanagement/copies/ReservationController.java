package librarymanagement.copies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.users.SecurityService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Reservations", description = "Customers can manage their reservations. Requires CUSTOMER role")
public class ReservationController {

    private final CopyService copyService;
    private final SecurityService securityService;

    public ReservationController(CopyService copyService, SecurityService securityService) {
        this.copyService = copyService;
        this.securityService = securityService;
    }

    @Operation(summary = "Get all my reservations",
            description = "Customers can view their reserved copies. Requires CUSTOMER role")
    @GetMapping("/api/reservations/mine")
    public Page<CopyResponse> getMyReservations(@ParameterObject Pageable pageable) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.getCopiesByCustomerId(customerId, pageable);
    }

    @Operation(summary = "Reserve a copy of a book",
            description = "Customers can reserve an available copy of a book by its ISBN, " +
                    "up to 3 active reservations. Requires CUSTOMER role")
    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public CopyResponse createReservation(@Valid @RequestBody CopyReservationRequest request) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), customerId);
    }

    @Operation(summary = "Cancel a reservation",
            description = "Customers can cancel their reservation for a copy. Requires CUSTOMER role")
    @DeleteMapping("/api/reservations/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@PathVariable Long copyId) {
        Long customerId = securityService.getCurrentCustomerId();
        copyService.cancelReservation(copyId, customerId);
    }
}
