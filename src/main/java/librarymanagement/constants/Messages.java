package librarymanagement.constants;

public class Messages {

    // Security
    public static final String SECURITY_LOGIN_FAILURE = "Login failed";
    public static final String SECURITY_LOGIN_REQUIRED = "You need to log in";
    // Lists
    public static final String SORT_FIELD_UNSUPPORTED = "Cannot sort by: ";
    public static final String SORT_FIELD_MISSING = "Sort needs a field";
    public static final String SORT_IGNORE_CASE_UNSUPPORTED = "Cannot sort ignoring case";
    // Book
    public static final String BOOK_NOT_FOUND = "Book not found with ISBN: ";
    public static final String BOOK_DUPLICATE = "A book with this ISBN already exists: ";
    public static final String BOOK_HAS_COPIES = "Cannot delete a book that has copies. ISBN: ";
    // Author
    public static final String AUTHOR_NOT_FOUND = "Author not found with name: ";
    // Copy
    public static final String COPY_NOT_FOUND = "Copy not found with ID: ";
    public static final String COPY_WRONG_CUSTOMER = "Copy is not currently used by the specified customer. Current customer: ";
    public static final String COPY_NO_AVAILABLE = "No available copies found for book with ISBN: ";
    public static final String COPY_NOT_BORROWED = "Copy is not currently borrowed. Current status: ";
    public static final String COPY_NOT_RESERVED = "Copy is not currently reserved. Current status: ";
    public static final String COPY_UNAVAILABLE_FOR_CHECKOUT = "Copy is not available for checkout. Current status: ";
    public static final String COPY_RESERVED_FOR_ANOTHER_CUSTOMER = "Copy is reserved for another customer. Customer ID: ";
    public static final String COPY_RESERVATION_LIMIT = "Reservation limit reached. Maximum active reservations: ";
    // Customer
    public static final String CUSTOMER_NOT_FOUND = "Customer not found with ID: ";
    public static final String CUSTOMER_EMAIL_REQUIRED = "A customer with an account must have an email";
    public static final String CUSTOMER_EMAIL_NEEDS_ACCOUNT = "A customer without an account cannot have an email";
    public static final String CUSTOMER_HAS_ACCOUNT = "Customer already has an account. Customer ID: ";
    // User
    public static final String USER_NOT_FOUND = "User not found: ";
    public static final String USER_EMAIL_DUPLICATE = "Email already exists: ";
    public static final String USER_PASSWORD_TOO_LONG = "Password cannot exceed 72 bytes";
    public static final String ACTIVATION_CODE_INVALID = "Activation code is invalid or expired";

    private Messages() {
    }
}
