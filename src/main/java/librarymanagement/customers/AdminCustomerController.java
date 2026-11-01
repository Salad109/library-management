package librarymanagement.customers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Customer Operations", description = "Librarians can do CRUD operations on customers. Requires LIBRARIAN role")
public class AdminCustomerController {

    private final CustomerService customerService;

    public AdminCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Get customers, optionally filtered by last name",
            description = "lastName matches a case-insensitive substring.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/customers")
    public Page<CustomerResponse> getCustomers(@RequestParam(required = false) String lastName, Pageable pageable) {
        if (lastName != null) {
            return customerService.getCustomersByLastName(lastName, pageable);
        }
        return customerService.getAllCustomers(pageable);
    }

    @Operation(summary = "Get a specific customer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/customers/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @Operation(summary = "Create a new customer",
            description = "Creates a new \"offline\" customer without a user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "409", description = "Customer with this email already exists")
    })
    @PostMapping("/api/admin/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        return customerService.addCustomer(customerRequest);
    }

    @Operation(summary = "Update an existing customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Customer with this email already exists")
    })
    @PutMapping("/api/admin/customers/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest customerRequest) {
        return customerService.updateCustomer(id, customerRequest);
    }
}
