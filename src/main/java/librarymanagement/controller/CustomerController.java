package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.model.Copy;
import librarymanagement.model.Customer;
import librarymanagement.service.CopyService;
import librarymanagement.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustomerController {

    private final CustomerService customerService;
    private final CopyService copyService;

    public CustomerController(CustomerService customerService, CopyService copyService) {
        this.customerService = customerService;
        this.copyService = copyService;
    }

    @GetMapping("/api/customers")
    public Page<Customer> getCustomers(Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/api/customers/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or @securityService.isCurrentUser(#id)")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @GetMapping("/api/customers/{id}/copies")
    @PreAuthorize("hasRole('LIBRARIAN') or @securityService.isCurrentUser(#id)")
    public Page<Copy> getCopiesByCustomerId(@PathVariable Long id, Pageable pageable) {
        return copyService.getCopiesByCustomerId(id, pageable);
    }

    @PostMapping("/api/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public Customer addCustomer(@Valid @RequestBody Customer customer) {
        return customerService.addCustomer(customer);
    }

    @PutMapping("/api/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }

    @DeleteMapping("/api/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }
}
