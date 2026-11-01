package librarymanagement.users;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class SecurityService {
    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found in the security context: " + username));
        if (user.getCustomer() == null) {
            throw new IllegalStateException("User does not have an associated customer: " + username);
        }
        return user.getCustomer().getId();
    }
}
