package librarymanagement.users;

import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    // Spring Security's "username" login is the email
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = User.normalizeEmail(username);
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("User not found with email: {}", email);
            return new UsernameNotFoundException(Messages.USER_NOT_FOUND + email);
        });
        // Librarians have no customer
        Long customerId = customerRepository.findByUserId(user.getId()).map(Customer::getId).orElse(null);
        return new AuthenticatedUser(user, customerId);
    }
}
