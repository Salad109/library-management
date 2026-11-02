package librarymanagement.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Authentication")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a customer account with an activation code",
            description = "Creates a CUSTOMER user for the existing customer the activation code belongs to.")
    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CustomerRegistrationRequest customerRegistrationRequest) {
        return userService.registerCustomer(customerRegistrationRequest);
    }

    @Operation(summary = "Get current username and role", description = "Both are absent when not logged in.")
    @GetMapping("/api/whoami")
    public WhoAmIResponse whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return new WhoAmIResponse(null, null);
        }
        return new WhoAmIResponse(auth.getName(), Role.of(auth));
    }

    @Operation(summary = "User login", description = "Authenticate with username and password using form data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    schema = @Schema(implementation = LoginForm.class))))
    @PostMapping("/api/login")
    public LoginResponse login() {
        // This method is never used.
        // It's here purely for documentation and monitoring purposes.
        return null;
    }

    @PostMapping("/api/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Same as login.
    }

    // Documents the form Spring Security reads
    record LoginForm(@NotBlank String username, @NotBlank String password) {
    }
}
