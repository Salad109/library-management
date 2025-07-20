package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Customer;
import librarymanagement.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Page<Customer> getAllCustomers(Pageable pageable) {
        log.debug("Fetching all customers, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Customer> customers = customerRepository.findAll(pageable);
        log.debug("Retrieved {} customers out of {} total", customers.getNumberOfElements(), customers.getTotalElements());
        return customers;
    }

    public Customer getCustomerById(Long id) {
        log.debug("Looking up customer by ID: {}", id);
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            log.debug("Customer not found with ID: {}", id);
            throw new ResourceNotFoundException(Messages.CUSTOMER_NOT_FOUND + id);
        }
        Customer existingCustomer = customer.get();
        log.debug("Found customer: {} {} with ID: {}", existingCustomer.getFirstName(), existingCustomer.getLastName(), id);
        return existingCustomer;
    }

    public Customer addCustomer(Customer customer) {
        log.info("Adding new customer: {} {}", customer.getFirstName(), customer.getLastName());
        String email = customer.getEmail();
        if (email != null && !email.isBlank()) {
            Optional<Customer> duplicateCustomer = customerRepository.findByEmail(email);
            if (duplicateCustomer.isPresent()) {
                log.debug("Duplicate customer found with email: {}", email);
                throw new DuplicateResourceException(Messages.CUSTOMER_EMAIL_DUPLICATE + email);
            }
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer added with ID: {}", savedCustomer.getId());
        return savedCustomer;
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        log.info("Updating customer with ID: {}", id);
        Customer existingCustomer = getCustomerById(id);

        String newEmail = customer.getEmail();

        if (newEmail != null && !newEmail.isBlank()) {
            Optional<Customer> duplicateCustomer = customerRepository.findByEmail(newEmail);
            if (duplicateCustomer.isPresent() && !duplicateCustomer.get().getId().equals(id)) {
                log.debug("Attempted to update customer with duplicate email: {}", newEmail);
                throw new DuplicateResourceException(Messages.CUSTOMER_EMAIL_DUPLICATE + newEmail);
            }
        }

        existingCustomer.setFirstName(customer.getFirstName());
        existingCustomer.setLastName(customer.getLastName());
        existingCustomer.setEmail(customer.getEmail());
        Customer savedCustomer = customerRepository.save(existingCustomer);
        log.info("Customer updated with ID: {}", savedCustomer.getId());
        return savedCustomer;
    }

}
