package librarymanagement.constants;

import librarymanagement.model.CopyStatus;
import librarymanagement.model.Role;

import java.util.Arrays;

public class Messages {
    // Security
    public static final String SECURITY_LOGIN_SUCCESS = "Login successful";
    public static final String SECURITY_LOGIN_FAILURE = "Login failed";
    public static final String SECURITY_LOGOUT_SUCCESS = "Logout successful";

    // Book
    public static final String BOOK_CHANGE_ISBN = "Cannot change ISBN of an existing book";
    public static final String BOOK_NULL_ISBN = "Book ISBN cannot be null";

    // Error
    public static final String ERROR_INVALID_COPY_STATUS = "Invalid status. Must be one of: " + Arrays.toString(CopyStatus.values())
            .replace("[", "")
            .replace("]", "")
            .replace(",", ", ");

    public static final String ERROR_INVALID_ROLE = "Invalid role. Must be one of: " + Arrays.toString(Role.values())
            .replace("[", "")
            .replace("]", "")
            .replace(",", ", ");
}
