package librarymanagement.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.CopyReservationRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.CopyService;
import librarymanagement.service.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Custom Reservations", description = "Customers can manage their reservations. Requires CUSTOMER role")
public class ReservationController {

    private final CopyService copyService;
    private final SecurityService securityService;

    public ReservationController(CopyService copyService, SecurityService securityService) {
        this.copyService = copyService;
        this.securityService = securityService;
    }

    @GetMapping("/api/reservations/mine")
    public Page<Copy> getMyReservations(Pageable pageable) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.getCopiesByCustomerId(customerId, pageable);
    }

    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Copy createReservation(@Valid @RequestBody CopyReservationRequest request) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), customerId);
    }

    @DeleteMapping("/api/reservations/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Copy cancelReservation(@PathVariable Long copyId) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.cancelReservation(copyId, customerId);
    }
}
