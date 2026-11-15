package librarymanagement.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LibrarianCreateRequest(
        @NotNull @Pattern(regexp = User.EMAIL_REGEX, message = User.EMAIL_INVALID) @Size(max = 150) String email,
        @NotBlank String password) {

    public LibrarianCreateRequest {
        email = User.normalizeEmail(email);
    }
}
