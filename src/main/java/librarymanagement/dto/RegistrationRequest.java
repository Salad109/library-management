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
}
