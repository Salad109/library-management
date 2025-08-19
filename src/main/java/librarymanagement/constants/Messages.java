package librarymanagement.constants;

public class Messages {

    private Messages() {
    }

    // Security
    public static final String SECURITY_LOGIN_SUCCESS = "Login successful";
    public static final String SECURITY_LOGIN_FAILURE = "Login failed";

    // Book
    public static final String BOOK_NULL_ISBN = "Book ISBN cannot be null";
    public static final String BOOK_NOT_FOUND = "Book not found with ISBN: ";
    public static final String BOOK_DUPLICATE = "A book with this ISBN already exists: ";
    public static final String BOOK_ISBN_VALIDATION_MESSAGE = "ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979";
    public static final String BOOK_TITLE_VALIDATION_MESSAGE = "Title cannot be blank";
    public static final String BOOK_PUBLICATION_YEAR_VALIDATION_MESSAGE = "Publication year must be a positive integer";
    public static final String BOOK_ISBN_REGEX = "^(?:\\d{9}[\\dX]|97[89]\\d{10})$";

    // Author
    public static final String AUTHOR_NOT_FOUND = "Author not found with name: ";
    public static final String AUTHOR_NAME_VALIDATION_MESSAGE = "Name cannot be blank";

    // Copy
    public static final String COPY_NOT_FOUND = "Copy not found with ID: ";
    public static final String COPY_WRONG_CUSTOMER = "Copy is not currently used by the specified customer. Current customer: ";
    public static final String COPY_NO_AVAILABLE = "No available copies found for book with ISBN: ";
    public static final String COPY_NOT_BORROWED = "Copy is not currently borrowed. Current status: ";
    public static final String COPY_NOT_RESERVED = "Copy is not currently reserved. Current status: ";
    public static final String COPY_UNAVAILABLE_FOR_CHECKOUT = "Copy is not available for checkout. Current status: ";
    public static final String COPY_RESERVED_FOR_ANOTHER_CUSTOMER = "Copy is reserved for another customer. Customer ID: ";
    public static final String COPY_BOOK_VALIDATION_MESSAGE = "Book cannot be null";
    public static final String COPY_STATUS_VALIDATION_MESSAGE = "Status cannot be null";
    public static final String COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE = "Minimum quantity must be at least 1";
    public static final String COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE = "Maximum quantity cannot exceed 100";

    // Customer
    public static final String CUSTOMER_NOT_FOUND = "Customer not found with ID: ";
    public static final String CUSTOMER_EMAIL_DUPLICATE = "Email already exists: ";
    public static final String CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE = "First name cannot be blank";
    public static final String CUSTOMER_LASTNAME_VALIDATION_MESSAGE = "Last name cannot be blank";
    public static final String CUSTOMER_EMAIL_VALIDATION_MESSAGE = "Email must be a valid email address";
    public static final String CUSTOMER_EMAIL_REGEX = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";

    // User
    public static final String USERNAME_NOT_FOUND = "User not found: ";
    public static final String USER_MISSING_CUSTOMER_FIELDS = "First name and last name are required for customers";
    public static final String USER_DUPLICATE = "User already exists with username: ";
    public static final String USER_USERNAME_VALIDATION_MESSAGE = "Username cannot be blank";
    public static final String USER_PASSWORD_VALIDATION_MESSAGE = "Password cannot be blank";
    public static final String USER_ROLE_VALIDATION_MESSAGE = "Role cannot be null";
    public static final String USER_FORBIDDEN_ACCESS = "This page is only accessible to employees. Go away";
}
