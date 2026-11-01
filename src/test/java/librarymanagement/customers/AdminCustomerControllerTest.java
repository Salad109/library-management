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
                    "lastName": "Mama",
                    "email": "joemamajr@example.com"
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
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("joemamajr@example.com");
    }

    @Test
    void testCreateInvalidCustomer() {
        String invalidCustomerJson = """
                {
                    "email": "goober@example.com"
                    }
                """;
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCustomerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "firstName", Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertFieldError(result, "lastName", Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @Test
    void testCreateCustomerWithTooLongFields() {
        MvcTestResult result = DataBuilder.createTestCustomer(mockMvcTester,
                "a".repeat(101),
                "a".repeat(101),
                "a".repeat(146) + "@x.io");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "firstName", Messages.CUSTOMER_FIRSTNAME_LENGTH_VALIDATION_MESSAGE);
        assertFieldError(result, "lastName", Messages.CUSTOMER_LASTNAME_LENGTH_VALIDATION_MESSAGE);
        assertFieldError(result, "email", Messages.CUSTOMER_EMAIL_LENGTH_VALIDATION_MESSAGE);
    }

    @Test
    void testCreateCustomerInvalidEmail() {
        MvcTestResult result = DataBuilder.createTestCustomer(mockMvcTester, "Goober", "III", "not-an-email");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "email", Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE);
    }

    @Test
    void testGetCustomerById() throws Exception {
        MvcTestResult customerCreationResult = DataBuilder.createTestCustomer(
                mockMvcTester,
                "Joe",
                "Mama",
                "joemama@example.com");
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
        MvcTestResult customerCreationResult = DataBuilder.createTestCustomer(
                mockMvcTester,
                "Joe",
                "Mama",
                "joemama@example.com");
        assertThat(customerCreationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractIdFromResponse(customerCreationResult);

        String updateJson = """
                {
                    "firstName": "The",
                    "lastName": "Goober",
                    "email": "goobermode@example.com"
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
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("goobermode@example.com");
    }

    @Test
    void testUpdateCustomerDuplicateEmail() throws Exception {
        MvcTestResult customer1CreationResult = DataBuilder.createTestCustomer(
                mockMvcTester,
                "Joe",
                "Mama",
                "joemama@example.com");
        assertThat(customer1CreationResult).hasStatus(HttpStatus.CREATED);

        int customer1Id = ControllerTestUtils.extractIdFromResponse(customer1CreationResult);

        MvcTestResult customer2CreationResult = DataBuilder.createTestCustomer(
                mockMvcTester,
                "The",
                "Goober",
                "goober@example.com");
        assertThat(customer2CreationResult).hasStatus(HttpStatus.CREATED);

        String updateJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "goober@example.com"
                    }
                """;
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/" + customer1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.CUSTOMER_EMAIL_DUPLICATE + "goober@example.com");
    }

    @Test
    void testFilterCustomersByLastName() {
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama", "joe@example.com"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Ann", "Other", "ann@example.com"))
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
}
