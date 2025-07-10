package librarymanagement.service;

import librarymanagement.model.User;
import librarymanagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isCurrentUser(Long customerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty() || optionalUser.get().getCustomer() == null) {
            return false;
        }
        return optionalUser.get().getCustomer().getId().equals(customerId);
    }
}
