package librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import librarymanagement.constants.Messages;
import librarymanagement.model.Role;

public record RegistrationRequest(
        @NotBlank(message = Messages.USER_USERNAME_VALIDATION_MESSAGE) String username,
        @NotBlank(message = Messages.USER_PASSWORD_VALIDATION_MESSAGE) String password,
        @NotNull(message = Messages.USER_ROLE_VALIDATION_MESSAGE) Role role,
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
