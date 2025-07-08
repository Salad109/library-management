package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/register", "/api/whoami").permitAll()
                        .requestMatchers("/api/books").authenticated()
                        .requestMatchers("/api/copies").hasRole("LIBRARIAN") // todo placeholder
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler(this::loginSuccess)
                        .failureHandler(this::loginFailure)
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
}