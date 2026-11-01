package librarymanagement.users;

import org.jspecify.annotations.Nullable;

/**
 * Both fields are null when nobody is logged in.
 */
public record WhoAmIResponse(@Nullable String username, @Nullable Role role) {
}
