package librarymanagement.customers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

public record CustomerRequest(
        @NotBlank(message = Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE) String firstName,
        @NotBlank(message = Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE) String lastName,
        @Pattern(regexp = Messages.CUSTOMER_EMAIL_REGEX, message = Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE) String email
) {
}
