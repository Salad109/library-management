package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.model.Customer;
import librarymanagement.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/api/customers")
    public Page<Customer> getCustomers(Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/api/customers/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
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
