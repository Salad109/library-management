package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import librarymanagement.constants.Messages;
import librarymanagement.users.LoginResponse;
import librarymanagement.users.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Filter errors never reach a controller; this hands them to GlobalExceptionHandler for the same error body
    private final HandlerExceptionResolver exceptionResolver;
    private final JsonMapper jsonMapper;

    public SecurityConfig(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                          JsonMapper jsonMapper) {
        this.exceptionResolver = exceptionResolver;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/*.html", "/assets/**", "/favicon.ico",
                                "/actuator/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api/register",
                                "/api/login",
                                "/api/books/**",
                                "/api/authors/**",
                                "/api/whoami").permitAll()
                        .requestMatchers("/api/reservations/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**", "/api/desk/**").hasRole("LIBRARIAN")
                        .anyRequest().hasRole("LIBRARIAN")
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/api/login")
                        .usernameParameter("email")
                        .successHandler(this::loginSuccessHandler)
                        .failureHandler(this::loginFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(this::authenticationEntryPoint)
                        .accessDeniedHandler(this::accessDeniedHandler)
                )
                .csrf(CsrfConfigurer::spa)
                .build();
    }

    private void loginSuccessHandler(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new LoginResponse(auth.getName(), Role.of(auth)));
    }

    private void loginFailureHandler(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        exceptionResolver.resolveException(request, response, null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, Messages.SECURITY_LOGIN_FAILURE));
    }

    private void authenticationEntryPoint(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        exceptionResolver.resolveException(request, response, null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, Messages.SECURITY_LOGIN_REQUIRED));
    }

    private void accessDeniedHandler(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        exceptionResolver.resolveException(request, response, null, ex);
    }
}
