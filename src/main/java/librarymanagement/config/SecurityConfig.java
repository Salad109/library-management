package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import librarymanagement.constants.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
                        .requestMatchers("/api/register", "/api/login", "/api/logout", "/api/whoami").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/authors/**").permitAll()

                        .requestMatchers("/actuator/**").permitAll()

                        // Authentication required for everything else
                        .anyRequest().authenticated()
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
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private void loginSuccess(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) throws java.io.IOException {
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGIN_SUCCESS);
    }

    private void loginFailure(HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException exception) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGIN_FAILURE);
    }

    private void logoutSuccess(HttpServletRequest request,
                               HttpServletResponse response,
                               Authentication authentication) throws java.io.IOException {
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGOUT_SUCCESS);
    }
}