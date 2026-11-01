package librarymanagement.users;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ActivationCodeResponse(@NotNull String activationCode, @NotNull Instant expiresAt) {
}
