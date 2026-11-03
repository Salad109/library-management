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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
    @Value("${library.admin.email}")
    protected String librarianEmail;

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
        return login("/admin/login", librarianEmail, librarianPassword, HttpStatus.FOUND);
    }

    /**
     * The library card flow: a librarian creates the customer and an activation code, the customer registers with it
     * and logs in through the API login.
     */
    protected CustomerAccount customerAccount(String email, String firstName, String lastName) {
        MockHttpSession librarianSession = librarianSession();
        long customerId = createCustomer(librarianSession, firstName, lastName);
        String code = activationCode(librarianSession, customerId);

        MvcTestResult registration = register(code, email, CUSTOMER_PASSWORD);
        assertThat(registration).hasStatus(HttpStatus.CREATED);

        return new CustomerAccount(customerId, login("/api/login", email, CUSTOMER_PASSWORD, HttpStatus.OK));
    }

    /**
     * Creates a customer at the desk (no account yet) and returns its ID.
     */
    protected long createCustomer(MockHttpSession librarianSession, String firstName, String lastName) {
        MvcTestResult result = mvc.post()
                .uri("/api/admin/customers")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "%s", "lastName": "%s"}
                        """.formatted(firstName, lastName))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return readJson(result).get("id").asLong();
    }

    protected String activationCode(MockHttpSession librarianSession, long customerId) {
        MvcTestResult result = mvc.post()
                .uri("/api/admin/customers/{id}/activation-code", customerId)
                .session(librarianSession)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return readJson(result).get("activationCode").asString();
    }

    protected MvcTestResult register(String activationCode, String email, String password) {
        return mvc.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"activationCode": "%s", "email": "%s", "password": "%s"}
                        """.formatted(activationCode, email, password))
                .exchange();
    }

    /**
     * A 400 whose ProblemDetail errors member has the message for the field.
     */
    protected static void assertFieldError(MvcTestResult result, String field, String message) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("errors")
                .asArray()
                .contains(Map.of("detail", message, "pointer", "#/" + field));
    }

    private static JsonNode readJson(MvcTestResult result) {
        return new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
    }

    protected MockHttpSession login(String uri, String email, String password, HttpStatus expectedStatus) {
        MvcTestResult result = mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
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
