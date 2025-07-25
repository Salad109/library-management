package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.CopyCheckoutRequest;
import librarymanagement.dto.CopyMarkLostRequest;
import librarymanagement.dto.CopyReturnRequest;
import librarymanagement.model.Copy;
import librarymanagement.model.Customer;
import librarymanagement.service.CopyService;
import librarymanagement.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Desk Operations", description = "Librarians can manage copy checkouts, returns, and lost copies. Requires LIBRARIAN role")
public class DeskController {

    private final CopyService copyService;
    private final CustomerService customerService;

    public DeskController(CopyService copyService, CustomerService customerService) {
        this.copyService = copyService;
        this.customerService = customerService;
    }

    @PostMapping("/api/desk/checkout")
    public Copy checkout(@Valid @RequestBody CopyCheckoutRequest request) {
        return copyService.checkout(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/return")
    public Copy returnCopy(@Valid @RequestBody CopyReturnRequest request) {
        return copyService.returnCopy(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/mark-lost")
    public Copy markLost(@Valid @RequestBody CopyMarkLostRequest request) {
        return copyService.markLost(request.copyId());
    }


    @Operation(summary = "Create a new customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "409", description = "Customer with this email already exists")
    })
    @PostMapping("/api/desk/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@Valid @RequestBody Customer customer) {
        return customerService.addCustomer(customer);
    }
}
