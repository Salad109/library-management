package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.ReservationRequest;
import librarymanagement.service.CopyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private final CopyService copyService;

    public ReservationController(CopyService copyService) {
        this.copyService = copyService;
    }

    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createReservation(@Valid @RequestBody ReservationRequest request) {
        copyService.reserveAnyAvailableCopy(request.bookIsbn(), request.customerId());
    }
}
