package librarymanagement.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;

public record LibrarianCreateRequest(
        @NotBlank(message = Messages.USER_USERNAME_VALIDATION_MESSAGE)
        @Size(max = 50, message = Messages.USER_USERNAME_LENGTH_VALIDATION_MESSAGE) String username,
        @NotBlank(message = Messages.USER_PASSWORD_VALIDATION_MESSAGE) String password) {

    public LibrarianCreateRequest {
        if (username != null) {
            username = username.trim();
        }
    }
}
