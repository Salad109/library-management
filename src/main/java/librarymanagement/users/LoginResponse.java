package librarymanagement.users;

import jakarta.validation.constraints.NotNull;

public record LoginResponse(@NotNull String username, @NotNull Role role) {
}
