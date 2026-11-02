package librarymanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import librarymanagement.constants.Messages;
import librarymanagement.users.LoginResponse;
import librarymanagement.users.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

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
    @Order(1)
    public SecurityFilterChain staffChain(HttpSecurity http) {
        return http
                .securityMatcher("/admin/**", "/login")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .anyRequest().hasRole("LIBRARIAN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/admin/login")
                        .successHandler(this::staffSuccessHandler)
                        .failureHandler(this::staffFailureHandler)
                )
                .logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/login"))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login.html", "/assets/**", "/favicon.ico",
                                "/actuator/**",
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
                        .loginProcessingUrl("/api/login")
                        .successHandler(this::apiSuccessHandler)
                        .failureHandler(this::apiFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                )
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(this::apiEntryPoint, pathPattern("/api/**"))
                        .accessDeniedHandler(this::accessDeniedHandler)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private void staffSuccessHandler(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        response.sendRedirect("/admin");
    }

    private void staffFailureHandler(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        response.sendRedirect("/login?error=true");
    }

    private void apiSuccessHandler(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new LoginResponse(auth.getName(), Role.of(auth)));
    }

    private void apiFailureHandler(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        exceptionResolver.resolveException(request, response, null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, Messages.SECURITY_LOGIN_FAILURE));
    }

    private void apiEntryPoint(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        exceptionResolver.resolveException(request, response, null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, Messages.SECURITY_LOGIN_REQUIRED));
    }

    private void accessDeniedHandler(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        exceptionResolver.resolveException(request, response, null, ex);
    }
}
