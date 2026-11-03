package librarymanagement.customers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;
import librarymanagement.users.User;

public record CustomerUpdateRequest(
        @NotBlank(message = Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_FIRSTNAME_LENGTH_VALIDATION_MESSAGE) String firstName,
        @NotBlank(message = Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_LASTNAME_LENGTH_VALIDATION_MESSAGE) String lastName,
        @Pattern(regexp = Messages.USER_EMAIL_REGEX, message = Messages.USER_EMAIL_VALIDATION_MESSAGE)
        @Size(max = 150, message = Messages.USER_EMAIL_LENGTH_VALIDATION_MESSAGE) String email
) {

    public CustomerUpdateRequest {
        email = User.normalizeEmail(email);
    }
}
