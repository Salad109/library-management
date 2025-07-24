package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.dto.UserRegistrationRequest;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.model.Customer;
import librarymanagement.model.User;
import librarymanagement.repository.CustomerRepository;
import librarymanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public User addUser(UserRegistrationRequest request) {
        log.info("Adding new user with username: {}", request.username());
        Optional<User> existingUser = userRepository.findByUsername(request.username());
        if (existingUser.isPresent()) {
            log.warn("Duplicate user found with username: {}", request.username());
            throw new DuplicateResourceException(Messages.USER_DUPLICATE + request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        if (request.isCustomer()) {
            if (!request.hasRequiredFieldsForCustomer()) {
                log.warn("User registration request missing required customer fields");
                throw new IllegalArgumentException(Messages.USER_MISSING_CUSTOMER_FIELDS);
            }
            Customer customer = new Customer();
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            customer.setEmail(request.email());
            customer = customerRepository.save(customer);
            user.setCustomer(customer);
        }

        User savedUser = userRepository.save(user);
        log.info("User added with username: {}", savedUser.getUsername());
        return savedUser;
    }
}
