package librarymanagement.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;

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

    @Operation(summary = "Get current username and role", description = "Both are null when not logged in.")
    @GetMapping("/api/whoami")
    public WhoAmIResponse whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return new WhoAmIResponse(null, null);
        }
        Set<String> authorities = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        Role role = Arrays.stream(Role.values())
                .filter(r -> authorities.contains(r.name()))
                .findFirst()
                .orElse(null);
        return new WhoAmIResponse(auth.getName(), role);
    }

    @Operation(summary = "User login", description = "Authenticate with username and password using form data")
    @PostMapping("/api/login")
    public String login() {
        // This method is never used.
        // It's here purely for documentation and monitoring purposes.
        return null;
    }

    @PostMapping("/api/logout")
    public String logout() {
        // Same as login.
        return null;
    }
}