package librarymanagement.users;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Librarian Operations", description = "Librarians can create other librarian accounts. Requires LIBRARIAN role")
public class AdminLibrarianController {

    private final UserService userService;

    public AdminLibrarianController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/admin/librarians")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createLibrarian(@Valid @RequestBody LibrarianCreateRequest librarianCreateRequest) {
        return userService.addLibrarian(librarianCreateRequest);
    }
}
