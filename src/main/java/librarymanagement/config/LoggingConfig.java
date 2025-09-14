package librarymanagement.config;

import jakarta.servlet.*;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoggingConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put("user", auth.getName());
                MDC.put("roles", auth.getAuthorities().toString());
                MDC.put("ip", request.getRemoteAddr());
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}