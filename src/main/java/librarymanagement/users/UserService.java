package librarymanagement.users;

import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

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

    @Transactional(readOnly = true)
    public boolean librarianExists() {
        return userRepository.existsByRole(Role.ROLE_LIBRARIAN);
    }

    @Transactional
    public UserResponse addLibrarian(LibrarianCreateRequest request) {
        log.debug("Adding new librarian with username: {}", request.username());
        User user = newUser(request.username(), request.password(), Role.ROLE_LIBRARIAN);

        User savedUser = userRepository.save(user);
        log.info("Librarian added with username: {}", savedUser.getUsername());
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse registerCustomer(CustomerRegistrationRequest request) {
        log.debug("Registering new customer with username: {}", request.username());
        User user = newUser(request.username(), request.password(), Role.ROLE_CUSTOMER);

        if (request.email() != null && !request.email().isBlank() && customerRepository.existsByEmail(request.email())) {
            log.warn("Attempted to register customer with duplicate email: {}", request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CUSTOMER_EMAIL_DUPLICATE + request.email());
        }

        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        user.setCustomer(customerRepository.save(customer));

        User savedUser = userRepository.save(user);
        log.info("Customer registered with username: {}", savedUser.getUsername());
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
