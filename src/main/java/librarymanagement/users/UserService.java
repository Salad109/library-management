package librarymanagement.users;

import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerRepository;
import librarymanagement.customers.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Duration ACTIVATION_CODE_VALIDITY = Duration.ofHours(48);
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       CustomerService customerService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean librarianExists() {
        return userRepository.existsByRole(Role.ROLE_LIBRARIAN);
    }

    @Transactional
    public UserResponse addLibrarian(LibrarianCreateRequest request) {
        User user = newUser(request.username(), request.password(), Role.ROLE_LIBRARIAN);

        User savedUser = userRepository.save(user);
        log.info("Librarian added with username: {}", savedUser.getUsername());
        return UserResponse.from(savedUser);
    }

    /**
     * One-time code for an existing customer to register online with. Replaces any earlier code.
     */
    @Transactional
    public ActivationCodeResponse generateActivationCode(Long customerId) {
        Customer customer = customerService.getCustomerOrThrow(customerId);
        if (userRepository.existsByCustomerId(customerId)) {
            log.warn("Activation code requested for customer that already has an account, ID: {}", customerId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CUSTOMER_HAS_ACCOUNT + customerId);
        }

        String code = ActivationCodes.generate();
        Instant expiresAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(ACTIVATION_CODE_VALIDITY);
        customer.setActivationCodeHash(ActivationCodes.hash(code));
        customer.setActivationCodeExpiresAt(expiresAt);
        customerRepository.save(customer);
        log.info("Activation code generated for customer ID: {}", customerId);
        return new ActivationCodeResponse(code, expiresAt);
    }

    /**
     * Creates a CUSTOMER user for the customer the activation code belongs to, and nulls the code.
     */
    @Transactional
    public UserResponse registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = customerRepository.findByActivationCodeHash(ActivationCodes.hash(request.activationCode()))
                .orElseThrow(() -> {
                    log.warn("Customer registration for username {} failed: unknown activation code", request.username());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.ACTIVATION_CODE_INVALID);
                });
        if (customer.getActivationCodeExpiresAt().isBefore(Instant.now())) {
            log.warn("Customer registration for username {} failed: activation code expired for customer ID: {}",
                    request.username(), customer.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.ACTIVATION_CODE_INVALID);
        }

        User user = newUser(request.username(), request.password(), Role.ROLE_CUSTOMER);
        user.setCustomer(customer);
        customer.setActivationCodeHash(null);
        customer.setActivationCodeExpiresAt(null);
        customerRepository.save(customer);

        User savedUser = userRepository.save(user);
        log.info("Customer registered with username: {}, customer ID: {}", savedUser.getUsername(), customer.getId());
        return UserResponse.from(savedUser);
    }

    private User newUser(String username, String password, Role role) {
        // BCrypt hashes at most 72 bytes
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            log.warn("Password for new user exceeds 72 bytes");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.USER_PASSWORD_TOO_LONG);
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Duplicate user found with username: {}", username);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.USER_DUPLICATE + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }
}
