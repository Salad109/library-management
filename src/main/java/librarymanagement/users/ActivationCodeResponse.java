package librarymanagement.users;

import java.time.Instant;

public record ActivationCodeResponse(String activationCode, Instant expiresAt) {
}
