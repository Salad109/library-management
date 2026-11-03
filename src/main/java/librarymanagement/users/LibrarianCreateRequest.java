package librarymanagement.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;

public record LibrarianCreateRequest(
        @NotNull(message = Messages.USER_EMAIL_VALIDATION_MESSAGE)
        @Pattern(regexp = Messages.USER_EMAIL_REGEX, message = Messages.USER_EMAIL_VALIDATION_MESSAGE)
        @Size(max = 150, message = Messages.USER_EMAIL_LENGTH_VALIDATION_MESSAGE) String email,
        @NotBlank(message = Messages.USER_PASSWORD_VALIDATION_MESSAGE) String password) {

    public LibrarianCreateRequest {
        email = User.normalizeEmail(email);
    }
}
