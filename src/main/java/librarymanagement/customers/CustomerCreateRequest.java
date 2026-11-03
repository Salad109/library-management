package librarymanagement.customers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;

public record CustomerCreateRequest(
        @NotBlank(message = Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_FIRSTNAME_LENGTH_VALIDATION_MESSAGE) String firstName,
        @NotBlank(message = Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.CUSTOMER_LASTNAME_LENGTH_VALIDATION_MESSAGE) String lastName
) {
}
