package librarymanagement.service;

import librarymanagement.model.User;
import librarymanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);
    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Getting customer ID for authenticated user: {}", username);

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            log.error("User not found in security context: {}", username);
            throw new IllegalStateException("User not found in the security context");
        }
        if (optionalUser.get().getCustomer() == null) {
            log.warn("User {} does not have an associated customer", username);
            throw new IllegalStateException("User does not have an associated customer");
        }

        Long customerId = optionalUser.get().getCustomer().getId();
        log.debug("Found customer ID: {} for user: {}", customerId, username);
        return customerId;
    }
}
