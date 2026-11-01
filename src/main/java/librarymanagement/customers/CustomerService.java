package librarymanagement.customers;

import librarymanagement.constants.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return loadPage(customerRepository.findAllIds(pageable));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return CustomerResponse.from(getCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomersByLastName(String lastName, Pageable pageable) {
        return loadPage(customerRepository.findIdsByLastName(lastName, pageable));
    }

    @Transactional
    public CustomerResponse addCustomer(CustomerRequest request) {
        String email = request.email();
        if (email != null && !email.isBlank() && customerRepository.existsByEmail(email)) {
            log.warn("Duplicate customer found with email: {}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CUSTOMER_EMAIL_DUPLICATE + email);
        }

        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer added with ID: {}", savedCustomer.getId());
        return CustomerResponse.from(savedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existingCustomer = getCustomerOrThrow(id);

        String newEmail = request.email();

        if (newEmail != null && !newEmail.isBlank()) {
            Optional<Customer> duplicateCustomer = customerRepository.findByEmail(newEmail);
            if (duplicateCustomer.isPresent() && !duplicateCustomer.get().getId().equals(id)) {
                log.warn("Attempted to update customer with duplicate email: {}", newEmail);
                throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CUSTOMER_EMAIL_DUPLICATE + newEmail);
            }
        }

        existingCustomer.setFirstName(request.firstName());
        existingCustomer.setLastName(request.lastName());
        existingCustomer.setEmail(request.email());
        Customer savedCustomer = customerRepository.save(existingCustomer);
        log.info("Customer updated with ID: {}", savedCustomer.getId());
        return CustomerResponse.from(savedCustomer);
    }

    // Helpers

    public Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> {
            log.warn("Customer not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.CUSTOMER_NOT_FOUND + id);
        });
    }

    private Page<CustomerResponse> loadPage(Page<Long> idPage) {
        if (idPage.isEmpty()) {
            return Page.empty(idPage.getPageable());
        }
        List<CustomerResponse> customers = customerRepository.findByIds(idPage.getContent()).stream()
                .map(CustomerResponse::from)
                .toList();
        return new PageImpl<>(customers, idPage.getPageable(), idPage.getTotalElements());
    }
}
