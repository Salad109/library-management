package librarymanagement.copies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.users.AuthenticatedUser;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Reservations", description = "Customers can manage their reservations. Requires CUSTOMER role")
public class ReservationController {

    private final CopyService copyService;

    public ReservationController(CopyService copyService) {
        this.copyService = copyService;
    }

    @Operation(summary = "Get all my reservations",
            description = "Customers can view their reserved copies. Requires CUSTOMER role")
    @GetMapping("/api/reservations/mine")
    public Page<CopyResponse> getMyReservations(@AuthenticationPrincipal AuthenticatedUser user,
                                                @ParameterObject Pageable pageable) {
        return copyService.getCopiesByCustomerId(user.getCustomerId(), pageable);
    }

    @Operation(summary = "Reserve a copy of a book",
            description = "Customers can reserve an available copy of a book by its ISBN, " +
                    "up to 3 active reservations. Requires CUSTOMER role")
    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public CopyResponse createReservation(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody CopyReservationRequest request) {
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), user.getCustomerId());
    }

    @Operation(summary = "Cancel a reservation",
            description = "Customers can cancel their reservation for a copy. Requires CUSTOMER role")
    @DeleteMapping("/api/reservations/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long copyId) {
        copyService.cancelReservation(copyId, user.getCustomerId());
    }
}
