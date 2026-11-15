package librarymanagement.customers;

import librarymanagement.constants.Messages;
import librarymanagement.users.User;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "LIBRARIAN")
class AdminCustomerControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateCustomerIgnoresEmail() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Joe Jr.", "lastName": "Mama", "email": "joe@example.com"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo("Joe Jr.");
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo("Mama");
        assertThat(result).bodyJson().doesNotHavePath("email");
    }

    @Test
    void testCreateCustomerValidation() {
        MvcTestResult result = DataBuilder.createTestCustomer(mockMvcTester, " ", "a".repeat(101));

        assertFieldError(result, "firstName", "must not be blank");
        assertFieldError(result, "lastName", "size must be between 0 and 100");
    }

    @Test
    void testUpdateCustomer() throws Exception {
        int customerId = ControllerTestUtils.extractIdFromResponse(
                DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama"));

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "The", "lastName": "Goober"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo("The");
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo("Goober");
        assertThat(result).bodyJson().doesNotHavePath("email");
    }

    @Test
    void testUpdateCustomerInvalidEmail() {
        long customerId = createCustomer(librarianSession(), "Joe", "Mama");

        assertFieldError(updateEmail(customerId, "\"not-an-email\""),
                "email", User.EMAIL_INVALID);
        assertFieldError(updateEmail(customerId, "\"" + "a".repeat(146) + "@x.io\""),
                "email", "size must be between 0 and 150");
    }

    @Test
    void testUpdateCustomerDuplicateEmail() {
        long customer1Id = customerAccount("joe@example.com", "Joe", "Mama").customerId();
        customerAccount("goober@example.com", "The", "Goober");

        assertThat(updateEmail(customer1Id, "\"goober@example.com\""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.USER_EMAIL_DUPLICATE + "goober@example.com");
    }

    @Test
    void testUpdateCustomerKeepingOwnEmail() {
        long customerId = customerAccount("joe@example.com", "Joe", "Mama").customerId();

        assertThat(updateEmail(customerId, "\"joe@example.com\"")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testUpdateCustomerWithoutAccountCannotGetEmail() {
        long customerId = createCustomer(librarianSession(), "Joe", "Mama");

        assertThat(updateEmail(customerId, "\"joe@example.com\""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.CUSTOMER_EMAIL_NEEDS_ACCOUNT);
    }

    @Test
    void testUpdateCustomerWithAccountCannotRemoveEmail() {
        long customerId = customerAccount("joe@example.com", "Joe", "Mama").customerId();

        assertThat(updateEmail(customerId, "null"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.CUSTOMER_EMAIL_REQUIRED);
    }

    @Test
    void testEmailChangeMovesTheLogin() {
        long customerId = customerAccount("joe@example.com", "Joe", "Mama").customerId();

        assertThat(updateEmail(customerId, "\"new.joe@example.com\""))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("email")
                .isEqualTo("new.joe@example.com");

        assertThat(mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=joe@example.com&password=" + CUSTOMER_PASSWORD))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        login("new.joe@example.com", CUSTOMER_PASSWORD);
    }

    @Test
    void testEmailShownInCustomerLookups() {
        long withAccount = customerAccount("joe@example.com", "Joe", "Mama").customerId();
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Ann", "Other")).hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/admin/customers/" + withAccount))
                .bodyJson()
                .extractingPath("email")
                .isEqualTo("joe@example.com");
        assertThat(mockMvcTester.get().uri("/api/admin/customers"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("content[0].lastName").isEqualTo("Mama");
                    assertThat(json).extractingPath("content[0].email").isEqualTo("joe@example.com");
                    assertThat(json).extractingPath("content[1].lastName").isEqualTo("Other");
                    assertThat(json).doesNotHavePath("content[1].email");
                });
    }

    @Test
    void testFilterCustomersByLastName() {
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama")).hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Jane", "Mama")).hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Ann", "Other")).hasStatus(HttpStatus.CREATED);

        // Page totals count every match, not just the page
        assertCustomerCount("/api/admin/customers?lastName=MAM&size=1", 2);
        assertCustomerCount("/api/admin/customers?size=1", 3);
        assertCustomerCount("/api/admin/customers?lastName=", 3);
    }

    private void assertCustomerCount(String uri, int expected) {
        assertThat(mockMvcTester.get().uri(uri))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(expected);
    }

    private MvcTestResult updateEmail(long customerId, String emailJson) {
        return mockMvcTester.put()
                .uri("/api/admin/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Joe", "lastName": "Mama", "email": %s}
                        """.formatted(emailJson))
                .exchange();
    }
}
