package librarymanagement.customers;

import librarymanagement.constants.Messages;
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
    void testCreateCustomer() {
        String customerJson = """
                {
                    "firstName": "Joe Jr.",
                    "lastName": "Mama"
                }
                """;
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo("Joe Jr.");
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo("Mama");
        assertThat(result).bodyJson().doesNotHavePath("email");
    }

    @Test
    void testCreateCustomerIgnoresEmail() {
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "joe@example.com"
                }
                """;
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().doesNotHavePath("email");
    }

    @Test
    void testCreateInvalidCustomer() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "firstName", Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertFieldError(result, "lastName", Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @Test
    void testCreateCustomerWithTooLongFields() {
        MvcTestResult result = DataBuilder.createTestCustomer(mockMvcTester,
                "a".repeat(101),
                "a".repeat(101));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "firstName", Messages.CUSTOMER_FIRSTNAME_LENGTH_VALIDATION_MESSAGE);
        assertFieldError(result, "lastName", Messages.CUSTOMER_LASTNAME_LENGTH_VALIDATION_MESSAGE);
    }

    @Test
    void testGetCustomerById() throws Exception {
        MvcTestResult customerCreationResult = DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama");
        assertThat(customerCreationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractIdFromResponse(customerCreationResult);

        assertThat(mockMvcTester.get().uri("/api/admin/customers/" + customerId))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetNonExistentCustomerById() {
        assertThat(mockMvcTester.get().uri("/api/admin/customers/9999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.CUSTOMER_NOT_FOUND + "9999");
    }

    @Test
    void testGetAllCustomers() {
        assertThat(mockMvcTester.get().uri("/api/admin/customers"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content")
                .isNotNull();
    }

    @Test
    void testUpdateCustomer() throws Exception {
        MvcTestResult customerCreationResult = DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama");
        assertThat(customerCreationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractIdFromResponse(customerCreationResult);

        String updateJson = """
                {
                    "firstName": "The",
                    "lastName": "Goober"
                }
                """;

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
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
                "email", Messages.USER_EMAIL_VALIDATION_MESSAGE);
        assertFieldError(updateEmail(customerId, "\"" + "a".repeat(146) + "@x.io\""),
                "email", Messages.USER_EMAIL_LENGTH_VALIDATION_MESSAGE);
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
    void testUpdateCustomerWithoutAccountCanLeaveEmailEmpty() {
        long customerId = createCustomer(librarianSession(), "Joe", "Mama");

        MvcTestResult result = updateEmail(customerId, "null");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().doesNotHavePath("email");
    }

    @Test
    void testUpdateCustomerWithoutAccountCannotGetEmail() {
        long customerId = createCustomer(librarianSession(), "Joe", "Mama");

        assertThat(updateEmail(customerId, "\"joe@example.com\""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.CUSTOMER_EMAIL_NEEDS_ACCOUNT);

        assertThat(mockMvcTester.get().uri("/api/admin/customers/" + customerId))
                .bodyJson()
                .doesNotHavePath("email");
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
        assertThat(updateEmail(customerId, "\"new.joe@example.com\"")).hasStatus(HttpStatus.OK);

        assertThat(mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=joe@example.com&password=" + CUSTOMER_PASSWORD))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        login("/api/login", "new.joe@example.com", CUSTOMER_PASSWORD, HttpStatus.OK);
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
    void testUpdateCustomerWithAccountCanChangeEmail() {
        long customerId = customerAccount("joe@example.com", "Joe", "Mama").customerId();

        MvcTestResult result = updateEmail(customerId, "\"fixed-typo@example.com\"");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("fixed-typo@example.com");
    }

    @Test
    void testFilterCustomersByLastName() {
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Ann", "Other"))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/admin/customers?lastName=MAM"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("page.totalElements").isEqualTo(1);
                    assertThat(json).extractingPath("content[0].lastName").isEqualTo("Mama");
                });
        assertThat(mockMvcTester.get().uri("/api/admin/customers?lastName="))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(2);
    }

    @Test
    void testCustomerPagesCountAllMatches() {
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama")).hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Jane", "Mama")).hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Ann", "Other")).hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/admin/customers?size=1"))
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(3);
        assertThat(mockMvcTester.get().uri("/api/admin/customers?lastName=mama&size=1"))
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(2);
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
