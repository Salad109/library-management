package librarymanagement.users;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 1, delay = 50)
    public User addUser(UserRegistrationRequest request) {
        log.debug("Adding new user with username: {}", request.username());
        Optional<User> existingUser = userRepository.findByUsername(request.username());
        if (existingUser.isPresent()) {
            log.warn("Duplicate user found with username: {}", request.username());
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.USER_DUPLICATE + request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        if (request.isCustomer()) {
            if (!request.hasRequiredFieldsForCustomer()) {
                log.warn("User registration request missing required customer fields");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.USER_MISSING_CUSTOMER_FIELDS);
            }
            Customer customer = new Customer();
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            if (request.email() != null && !request.email().isBlank() && customerRepository.existsByEmail(request.email())) {
                log.warn("Attempted to create user with duplicate email: {}", request.email());
                throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CUSTOMER_EMAIL_DUPLICATE + request.email());
            }

            customer.setEmail(request.email());
            customer = customerRepository.save(customer);
            user.setCustomer(customer);
        }

        User savedUser = userRepository.save(user);
        log.info("User added with username: {}", savedUser.getUsername());
        return savedUser;
    }
}
