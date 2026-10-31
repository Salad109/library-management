package librarymanagement.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;

public record CustomerRegistrationRequest(
        @NotBlank(message = Messages.USER_USERNAME_VALIDATION_MESSAGE)
        @Size(max = 50, message = Messages.USER_USERNAME_LENGTH_VALIDATION_MESSAGE) String username,
        @NotBlank(message = Messages.USER_PASSWORD_VALIDATION_MESSAGE) String password,
        @NotBlank(message = Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_FIRSTNAME_LENGTH_VALIDATION_MESSAGE) String firstName,
        @NotBlank(message = Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_LASTNAME_LENGTH_VALIDATION_MESSAGE) String lastName,
        @Pattern(regexp = Messages.CUSTOMER_EMAIL_REGEX, message = Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE)
        @Size(max = 150, message = Messages.CUSTOMER_EMAIL_LENGTH_VALIDATION_MESSAGE) String email) {

    public CustomerRegistrationRequest {
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
}
