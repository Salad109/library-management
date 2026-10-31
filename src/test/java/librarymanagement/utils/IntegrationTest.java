package librarymanagement.utils;

import librarymanagement.users.LibrarianBootstrap;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full application context against the Testcontainers Postgres. Tests are NOT transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
public abstract class IntegrationTest {

    protected static final String CUSTOMER_PASSWORD = "password123";

    /**
     * The account librarianSession() creates from library.admin.* in application-test.yml.
     */
    @Value("${library.admin.username}")
    protected String librarianUsername;

    @Value("${library.admin.password}")
    protected String librarianPassword;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private LibrarianBootstrap librarianBootstrap;

    @BeforeEach
    void resetDatabaseAndCaches() {
        jdbcTemplate.execute("TRUNCATE authors, books, books_authors, copies, customers, users RESTART IDENTITY CASCADE");
        // Caches outlive the rows they were built from
        cacheManager.getCacheNames().forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    /**
     * Creates the librarian through the real startup bootstrap and logs in through the staff login.
     */
    protected MockHttpSession librarianSession() {
        librarianBootstrap.run(new DefaultApplicationArguments());
        return login("/admin/login", librarianUsername, librarianPassword, HttpStatus.FOUND);
    }

    /**
     * Registers a customer account and logs it in through the API login.
     */
    protected CustomerAccount customerAccount(String username, String firstName, String lastName) {
        String customerJson = """
                {
                    "username": "%s",
                    "password": "%s",
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """.formatted(username, CUSTOMER_PASSWORD, firstName, lastName);

        MvcTestResult registration = mvc.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();
        assertThat(registration).hasStatus(HttpStatus.CREATED);

        long customerId = new ObjectMapper().readTree(registration.getResponse().getContentAsByteArray())
                .get("customer").get("id").asLong();
        return new CustomerAccount(customerId, login("/api/login", username, CUSTOMER_PASSWORD, HttpStatus.OK));
    }

    private MockHttpSession login(String uri, String username, String password, HttpStatus expectedStatus) {
        MvcTestResult result = mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8))
                .exchange();
        assertThat(result).hasStatus(expectedStatus);

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    protected record CustomerAccount(long customerId, MockHttpSession session) {
    }
}
