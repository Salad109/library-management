package librarymanagement.users;

import librarymanagement.constants.Messages;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Activation codes: librarian generates one for a desk customer, the customer registers with it.
 */
class CustomerActivationTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testGenerateActivationCode() {
        MockHttpSession librarianSession = librarianSession();
        long customerId = createCustomer(librarianSession, "Joe", "Mama");

        MvcTestResult result = generateCode(librarianSession, customerId);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("activationCode").asString().matches("[0-9A-Z]{4}-[0-9A-Z]{4}");
        assertThat(result).bodyJson().extractingPath("expiresAt").asString()
                .satisfies(expiresAt -> assertThat(Instant.parse(expiresAt))
                        .isCloseTo(Instant.now().plus(Duration.ofHours(48)), within(Duration.ofMinutes(1))));
    }

    @Test
    void testCodeIsAcceptedInAnyCaseWithoutDash() {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));
        String typed = " " + code.replace("-", "").toLowerCase(Locale.ROOT) + " ";

        assertThat(register(typed, "joe", CUSTOMER_PASSWORD)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testRegisterWithWrongCode() {
        MockHttpSession librarianSession = librarianSession();
        activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));

        MvcTestResult result = register("0000-0000", "joe", CUSTOMER_PASSWORD);

        assertInvalidCode(result);
        assertThat(userRepository.findByUsername("joe")).isEmpty();
    }

    @Test
    void testRegisterWithExpiredCode() {
        MockHttpSession librarianSession = librarianSession();
        long customerId = createCustomer(librarianSession, "Joe", "Mama");
        String code = activationCode(librarianSession, customerId);
        jdbcTemplate.update("UPDATE customers SET activation_code_expires_at = now() - interval '1 minute' WHERE id = ?",
                customerId);

        assertInvalidCode(register(code, "joe", CUSTOMER_PASSWORD));
        assertThat(userRepository.findByUsername("joe")).isEmpty();
    }

    @Test
    void testCodeWorksOnce() {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));
        assertThat(register(code, "joe", CUSTOMER_PASSWORD)).hasStatus(HttpStatus.CREATED);

        assertInvalidCode(register(code, "joe2", CUSTOMER_PASSWORD));
    }

    @Test
    void testFailedRegistrationKeepsCode() {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));

        // Username taken by the librarian, code preserved
        assertThat(register(code, librarianUsername, CUSTOMER_PASSWORD)).hasStatus(HttpStatus.CONFLICT);

        assertThat(register(code, "joe", CUSTOMER_PASSWORD)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testNewCodeReplacesOld() {
        MockHttpSession librarianSession = librarianSession();
        long customerId = createCustomer(librarianSession, "Joe", "Mama");
        String oldCode = activationCode(librarianSession, customerId);
        String newCode = activationCode(librarianSession, customerId);

        assertInvalidCode(register(oldCode, "joe", CUSTOMER_PASSWORD));
        assertThat(register(newCode, "joe", CUSTOMER_PASSWORD)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testCannotGenerateCodeForCustomerWithAccount() {
        long customerId = customerAccount("joe", "Joe", "Mama").customerId();

        MvcTestResult result = generateCode(librarianSession(), customerId);

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.CUSTOMER_HAS_ACCOUNT + customerId);
    }

    @Test
    void testGenerateCodeForMissingCustomer() {
        MvcTestResult result = generateCode(librarianSession(), 999);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.CUSTOMER_NOT_FOUND + 999);
    }

    @Test
    void testCustomerCannotGenerateCode() {
        MockHttpSession librarianSession = librarianSession();
        long otherCustomerId = createCustomer(librarianSession, "Other", "Customer");
        MockHttpSession customerSession = customerAccount("joe", "Joe", "Mama").session();

        assertThat(generateCode(customerSession, otherCustomerId)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void testAnonymousCannotGenerateCode() {
        long customerId = createCustomer(librarianSession(), "Joe", "Mama");

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers/{id}/activation-code", customerId)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult generateCode(MockHttpSession session, long customerId) {
        return mockMvcTester.post()
                .uri("/api/admin/customers/{id}/activation-code", customerId)
                .session(session)
                .exchange();
    }

    private static void assertInvalidCode(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.ACTIVATION_CODE_INVALID);
    }
}
