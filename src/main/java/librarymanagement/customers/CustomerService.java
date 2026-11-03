package librarymanagement.customers;

import librarymanagement.constants.Messages;
import librarymanagement.users.User;
import librarymanagement.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return customerRepository.findAllResponses(pageable);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = getCustomerOrThrow(id);
        String email = userRepository.findByCustomerId(id).map(User::getEmail).orElse(null);
        return CustomerResponse.from(customer, email);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomersByLastName(String lastName, Pageable pageable) {
        return customerRepository.findResponsesByLastName(lastName, pageable);
    }

    @Transactional
    public CustomerResponse addCustomer(CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer added with ID: {}", savedCustomer.getId());
        return CustomerResponse.from(savedCustomer, null);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer existingCustomer = getCustomerOrThrow(id);
        User user = userRepository.findByCustomerId(id).orElse(null);
        String email = request.email();

        if (user == null && email != null) {
            log.warn("Attempted to give an email to a customer without an account, ID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.CUSTOMER_EMAIL_NEEDS_ACCOUNT);
        }
        if (user != null && email == null) {
            log.warn("Attempted to remove the email of customer ID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.CUSTOMER_EMAIL_REQUIRED);
        }
        if (user != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                log.warn("Attempted to change the email of customer ID: {} to one already in use: {}", id, email);
                throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.USER_EMAIL_DUPLICATE + email);
            }
            user.setEmail(email);
            userRepository.save(user);
        }

        existingCustomer.setFirstName(request.firstName());
        existingCustomer.setLastName(request.lastName());
        Customer savedCustomer = customerRepository.save(existingCustomer);
        log.info("Customer updated with ID: {}", savedCustomer.getId());
        return CustomerResponse.from(savedCustomer, email);
    }

    // Helpers

    public Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> {
            log.warn("Customer not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.CUSTOMER_NOT_FOUND + id);
        });
    }
}
