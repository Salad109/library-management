package librarymanagement.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Customer Operations")
public class AdminCustomerAccountController {

    private final UserService userService;

    public AdminCustomerAccountController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Generate an activation code for a customer",
            description = "One-time code the customer registers online with (POST /api/register). Valid for 48 hours. " +
                    "Generating a new code replaces the old one.")
    @PostMapping("/api/admin/customers/{id}/activation-code")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationCodeResponse generateActivationCode(@PathVariable Long id) {
        return userService.generateActivationCode(id);
    }
}
