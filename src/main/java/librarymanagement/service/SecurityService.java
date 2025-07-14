package librarymanagement.service;

import librarymanagement.model.User;
import librarymanagement.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new IllegalStateException("User not found in the security context");
        }
        if (optionalUser.get().getCustomer() == null) {
            throw new IllegalStateException("User does not have an associated customer");
        }
        return optionalUser.get().getCustomer().getId();
    }
}
