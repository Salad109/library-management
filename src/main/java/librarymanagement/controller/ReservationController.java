package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.ReservationRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.CopyService;
import librarymanagement.service.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private final CopyService copyService;
    private final SecurityService securityService;

    public ReservationController(CopyService copyService, SecurityService securityService) {
        this.copyService = copyService;
        this.securityService = securityService;
    }

    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public Copy createReservation(@Valid @RequestBody ReservationRequest request) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), customerId);
    }
}
