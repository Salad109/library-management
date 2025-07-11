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
    public static final String BOOK_NOT_FOUND = "Book not found with ISBN: ";
    public static final String BOOK_DUPLICATE = "A book with this ISBN already exists: ";

    // Error
    public static final String ERROR_INVALID_COPY_STATUS = "Invalid status. Must be one of: " + Arrays.toString(CopyStatus.values())
            .replace("[", "")
            .replace("]", "")
            .replace(",", ", ");

    public static final String ERROR_INVALID_ROLE = "Invalid role. Must be one of: " + Arrays.toString(Role.values())
            .replace("[", "")
            .replace("]", "")
            .replace(",", ", ");

    // Author
    public static final String AUTHOR_NOT_FOUND = "Author not found with name: ";

    // Copy
    public static final String COPY_NOT_FOUND = "Copy not found with ID: ";
    public static final String COPY_WRONG_CUSTOMER = "Copy is not currently used by the specified customer. Current customer: ";
    public static final String COPY_NO_AVAILABLE = "No available copies found for book with ISBN: ";
    public static final String COPY_NOT_BORROWED = "Copy is not currently borrowed. Current status: ";
    public static final String COPY_NOT_RESERVED = "Copy is not currently reserved. Current status: ";
    public static final String COPY_UNAVAILABLE_FOR_BORROWING = "Copy is not currently available for borrowing. Current status: ";
    public static final String COPY_UNAVAILABLE_FOR_RESERVATION = "Copy is not currently available for reservation. Current status: ";

    // Customer
    public static final String CUSTOMER_NOT_FOUND = "Customer not found with ID: ";
    public static final String CUSTOMER_EMAIL_DUPLICATE = "Email already exists: ";

    // User
    public static final String USERNAME_NOT_FOUND = "User not found: ";
    public static final String USER_MISSING_CUSTOMER_FIELDS = "First name and last name are required for customers";
    public static final String USER_DUPLICATE = "User already exists with username: ";

}
