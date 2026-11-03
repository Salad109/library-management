package librarymanagement.users;

import jakarta.validation.constraints.NotNull;
import librarymanagement.customers.CustomerResponse;

public record UserResponse(@NotNull Long id, @NotNull String email, @NotNull Role role, CustomerResponse customer) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCustomer() != null ? CustomerResponse.from(user.getCustomer(), user.getEmail()) : null);
    }
}
