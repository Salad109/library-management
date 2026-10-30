package librarymanagement.users;

import librarymanagement.customers.CustomerResponse;

public record UserResponse(Long id, String username, Role role, CustomerResponse customer) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCustomer() != null ? CustomerResponse.from(user.getCustomer()) : null);
    }
}
