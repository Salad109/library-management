package librarymanagement.users;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * The logged-in user held in the session carries the customer ID (null for librarians).
 */
public class AuthenticatedUser extends org.springframework.security.core.userdetails.User {

    private final Long customerId;

    public AuthenticatedUser(User user, Long customerId) {
        super(user.getEmail(), user.getPassword(), List.of(new SimpleGrantedAuthority(user.getRole().name())));
        this.customerId = customerId;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
