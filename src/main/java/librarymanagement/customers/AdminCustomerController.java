package librarymanagement.customers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
    @GetMapping("/api/admin/customers")
    public Page<CustomerResponse> getCustomers(@RequestParam(required = false) String lastName, @ParameterObject Pageable pageable) {
        if (lastName != null) {
            return customerService.getCustomersByLastName(lastName, pageable);
        }
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/api/admin/customers/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @Operation(summary = "Create a new customer",
            description = "Creates a new \"offline\" customer without a user account or email.")
    @PostMapping("/api/admin/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return customerService.addCustomer(request);
    }

    @Operation(summary = "Update a customer",
            description = "An email can be changed, but not added or removed: a customer gets one by registering online.")
    @PutMapping("/api/admin/customers/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.updateCustomer(id, request);
    }
}
