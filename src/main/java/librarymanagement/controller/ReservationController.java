package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.ReservationRequest;
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
        return copyService.getReservationsByCustomerId(customerId, pageable);
    }

    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Copy createReservation(@Valid @RequestBody ReservationRequest request) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), customerId);
    }

    @DeleteMapping("/api/reservations/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@PathVariable Long copyId) {
        Long customerId = securityService.getCurrentCustomerId();
        copyService.cancelReservation(copyId, customerId);
    }
}
