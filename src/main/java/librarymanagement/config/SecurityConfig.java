package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                        // Public endpoints like browsing
                        .requestMatchers("/api/register", "/api/whoami").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/authors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/copies/book/*/count").permitAll()

                        // Customer operations like managing their reservations
                        .requestMatchers(HttpMethod.POST, "/api/customers/*/reserve/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/copies/*/undo-reserve/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/customers/*/copies").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/customers/*").hasRole("CUSTOMER")

                        // Librarian can do everything else
                        .anyRequest().hasRole("LIBRARIAN")
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler(this::loginSuccess)
                        .failureHandler(this::loginFailure)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(this::logoutSuccess)
                )
                .csrf(csrf -> csrf.disable())
                .build();
    }

    private void loginSuccess(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) throws java.io.IOException {
        response.setContentType("text/plain");
        response.getWriter().write("Login successful!");
    }

    private void loginFailure(HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException exception) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("text/plain");
        response.getWriter().write("Login failed!");
    }

    private void logoutSuccess(HttpServletRequest request,
                               HttpServletResponse response,
                               Authentication authentication) throws java.io.IOException {
        response.setContentType("text/plain");
        response.getWriter().write("Logout successful!");
    }
}