package librarymanagement.users;

/**
 * Both fields are absent from the JSON when nobody is logged in.
 */
public record WhoAmIResponse(String username, Role role) {
}
