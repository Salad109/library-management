package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.dto.RegistrationRequest;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Customer;
import librarymanagement.model.Role;
import librarymanagement.model.User;
import librarymanagement.repository.CustomerRepository;
import librarymanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User addUser(RegistrationRequest request) {
        Optional<User> existingUser = userRepository.findByUsername(request.username());
        if (existingUser.isPresent()) {
            throw new DuplicateResourceException("User already exists with username: " + request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        if (request.role().equals(Role.ROLE_CUSTOMER)) {
            if (isNullOrBlank(request.firstName()) || isNullOrBlank(request.lastName())) {
                throw new IllegalArgumentException("First name and last name are required for customers.");
            }
            Customer customer = new Customer();
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            customer.setEmail(request.email());
            customer = customerRepository.save(customer);
            user.setCustomer(customer);
        }

        return userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found with username: " + username);
        }
        return user.get();
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }
}
