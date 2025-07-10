package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.RegistrationRequest;
import librarymanagement.model.User;
import librarymanagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return userService.addUser(registrationRequest);
    }

    @GetMapping("/api/whoami")
    public String whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "Logged in as: " + auth.getName() + " with roles: " + auth.getAuthorities();
    }
}