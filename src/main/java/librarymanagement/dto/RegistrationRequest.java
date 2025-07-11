package librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import librarymanagement.model.Role;

public record RegistrationRequest(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotBlank(message = "Password cannot be blank") String password,
        @NotNull(message = "Role cannot be null") Role role,
        String firstName,
        String lastName,
        String email) {

    public RegistrationRequest {
        if (username != null) {
            username = username.trim();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
        if (email != null) {
            email = email.trim();
        }
    }

    public boolean isCustomer() {
        return role == Role.ROLE_CUSTOMER;
    }

    public boolean hasRequiredFieldsForCustomer() {
        return firstName != null && !firstName.isBlank() &&
                lastName != null && !lastName.isBlank();
    }
}
