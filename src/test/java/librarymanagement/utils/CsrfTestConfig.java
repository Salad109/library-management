package librarymanagement.utils;

import jakarta.servlet.http.Cookie;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Every MockMvc request sends a CSRF token like the frontend does.
 */
@TestConfiguration(proxyBeanMethods = false)
public class CsrfTestConfig {

    @Bean
    MockMvcBuilderCustomizer csrfTokenOnEveryRequest() {
        return builder -> builder.defaultRequest(get("/")
                .cookie(new Cookie("XSRF-TOKEN", "test-token"))
                .header("X-XSRF-TOKEN", "test-token"));
    }
}
