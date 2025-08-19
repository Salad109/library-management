package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import librarymanagement.constants.Messages;
import librarymanagement.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/login", "/api/login",
                                "/api/register", "/api/books/**", "/api/authors/**").permitAll()

                        .requestMatchers("/admin/**", "/api/admin/**", "/api/desk/**").hasRole("LIBRARIAN")
                        .requestMatchers("/customer/**", "/api/reservations/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/login")
                        .successHandler(this::roleBasedRedirect)
                        .failureHandler(this::loginFailure)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private void roleBasedRedirect(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Authentication authentication) throws IOException {

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(Role.ROLE_LIBRARIAN.toString()));

        if (isLibrarian) {
            SavedRequest savedRequest = (SavedRequest) request.getSession()
                    .getAttribute("SPRING_SECURITY_SAVED_REQUEST");

            String targetUrl = savedRequest != null ?
                    savedRequest.getRedirectUrl() : "/admin";

            response.sendRedirect(targetUrl);
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("text/html");
            response.getWriter().write(Messages.USER_FORBIDDEN_ACCESS);
        }
    }

    private void loginFailure(HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException exception) throws IOException {
        response.sendRedirect("/login?error=true");
    }
}