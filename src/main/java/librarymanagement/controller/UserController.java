package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.UserRegistrationRequest;
import librarymanagement.model.User;
import librarymanagement.service.UserService;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Register a new user account",
            description = "Create a new user account. For CUSTOMER role, firstName and lastName are required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or missing required fields"),
            @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest) {
        return userService.addUser(userRegistrationRequest);
    }

    @Operation(summary = "Get current username and role")
    @ApiResponse(responseCode = "200", description = "User information retrieved successfully")
    @GetMapping("/api/whoami")
    public String whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "Logged in as: " + auth.getName() + " with roles: " + auth.getAuthorities();
    }

    @Operation(summary = "User login", description = "Authenticate with username and password using form data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/api/login")
    public String login() {
        // This method is never used.
        // It's here purely for documentation and monitoring purposes.
        return null;
    }

    @Operation(summary = "User logout")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    @PostMapping("/api/logout")
    public String logout() {
        // Same as login.
        return null;
    }
}