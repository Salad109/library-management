package librarymanagement.customers;

import librarymanagement.constants.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
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
        log.debug("Fetching all customers, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> idPage = customerRepository.findAllIds(pageable);

        if (idPage.getContent().isEmpty()) {
            log.debug("No customers found, returning empty page");
            return Page.empty(pageable);
        }

        List<CustomerResponse> customers = toResponses(customerRepository.findByIds(idPage.getContent()));

        log.debug("Retrieved {} customers out of {} total", customers.size(), idPage.getTotalElements());

        return new PageImpl<>(customers, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return CustomerResponse.from(getCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomersByLastName(String lastName, Pageable pageable) {
        log.debug("Searching customers by last name: {}, page: {}, size: {}", lastName, pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> idPage = customerRepository.findIdsByLastName(lastName, pageable);

        if (idPage.getContent().isEmpty()) {
            log.debug("No customers found with last name containing: {}", lastName);
            return Page.empty(pageable);
        }

        List<CustomerResponse> customers = toResponses(customerRepository.findByIds(idPage.getContent()));
        log.debug("Found {} customers with last name containing: {}", customers.size(), lastName);
        return new PageImpl<>(customers, pageable, idPage.getTotalElements());
    }

    @Transactional
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 1)
    public CustomerResponse addCustomer(CustomerRequest request) {
        log.debug("Adding new customer: {} {}", request.firstName(), request.lastName());
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
        log.debug("Updating customer with ID: {}", id);
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
        log.debug("Looking up customer by ID: {}", id);
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            log.warn("Customer not found with ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.CUSTOMER_NOT_FOUND + id);
        }
        Customer existingCustomer = customer.get();
        log.debug("Found customer: {} {} with ID: {}", existingCustomer.getFirstName(), existingCustomer.getLastName(), id);
        return existingCustomer;
    }

    private List<CustomerResponse> toResponses(List<Customer> customers) {
        return customers.stream().map(CustomerResponse::from).toList();
    }
}
