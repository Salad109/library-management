package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import librarymanagement.constants.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
    @Order(1)
    public SecurityFilterChain staffChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**", "/login")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .anyRequest().hasRole("LIBRARIAN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/login?error=true")
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**",
                                "/api/register",
                                "/api/login",
                                "/api/books/**",
                                "/api/authors/**").permitAll()
                        .requestMatchers("/api/reservations/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**", "/api/desk/**").hasRole("LIBRARIAN")
                        .anyRequest().hasRole("LIBRARIAN")
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler(this::apiSuccessHandler)
                        .failureHandler(this::apiFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(this::apiLogoutHandler)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private void apiSuccessHandler(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Authentication auth) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGIN_SUCCESS);
    }

    private void apiFailureHandler(HttpServletRequest request,
                                   HttpServletResponse response,
                                   AuthenticationException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGIN_FAILURE);
    }

    private void apiLogoutHandler(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Authentication auth) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/plain");
        response.getWriter().write(Messages.SECURITY_LOGOUT_SUCCESS);
    }
}