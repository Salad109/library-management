package librarymanagement.users;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates the first librarian on start from credentials in .env, if no librarians exist. Startup fails if none exist
 * and the credentials are missing or invalid.
 */
@Component
public class LibrarianBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LibrarianBootstrap.class);
    private final UserService userService;
    private final Validator validator;
    private final String email;
    private final String password;

    public LibrarianBootstrap(UserService userService,
                              Validator validator,
                              @Value("${library.admin.email:}") String email,
                              @Value("${library.admin.password:}") String password) {
        this.userService = userService;
        this.validator = validator;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.librarianExists()) {
            log.debug("A librarian already exists, skipping librarian bootstrap");
            return;
        }

        LibrarianCreateRequest request = new LibrarianCreateRequest(email, password);
        Set<ConstraintViolation<LibrarianCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            throw new IllegalStateException("Invalid LIBRARY_ADMIN_EMAIL / LIBRARY_ADMIN_PASSWORD: " + messages);
        }

        userService.addLibrarian(request);
        log.info("Created initial librarian: {}", request.email());
    }
}
