package librarymanagement.users;

import jakarta.validation.constraints.NotNull;
import librarymanagement.customers.Customer;

public record UserResponse(@NotNull Long id, @NotNull String email, @NotNull Role role, Long customerId) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), null);
    }

    public static UserResponse from(User user, Customer customer) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), customer.getId());
    }
}
