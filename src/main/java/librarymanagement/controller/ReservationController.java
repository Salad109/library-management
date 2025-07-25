package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Get all my reservations",
            description = "Customers can view their reserved copies. Requires CUSTOMER role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Requires CUSTOMER role")
    })
    @GetMapping("/api/reservations/mine")
    public Page<Copy> getMyReservations(Pageable pageable) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.getCopiesByCustomerId(customerId, pageable);
    }

    @Operation(summary = "Reserve a copy of a book",
            description = "Customers can reserve an available copy of a book by its ISBN. " +
                    "Requires CUSTOMER role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Requires CUSTOMER role"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN, or no available copies")
    })
    @PostMapping("/api/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Copy createReservation(@Valid @RequestBody CopyReservationRequest request) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.reserveAnyAvailableCopy(request.bookIsbn(), customerId);
    }

    @Operation(summary = "Cancel a reservation",
            description = "Customers can cancel their reservation for a copy. Requires CUSTOMER role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reservation cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data, or copy not reserved, " +
                    "or not reserved by this customer"),
            @ApiResponse(responseCode = "403", description = "Requires CUSTOMER role"),
            @ApiResponse(responseCode = "404", description = "Copy not found")
    })
    @DeleteMapping("/api/reservations/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Copy cancelReservation(@PathVariable Long copyId) {
        Long customerId = securityService.getCurrentCustomerId();
        return copyService.cancelReservation(copyId, customerId);
    }
}
