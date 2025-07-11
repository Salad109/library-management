package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testAddCustomer() {
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("firstName")
                .isEqualTo("Joe");
    }

    @Test
    void testAddCustomerWithMissingFields() {
        String customerJson = """
                {
                    "email": "goober@example.com"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("firstName")
                .isEqualTo(Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertThat(result)
                .bodyJson()
                .extractingPath("lastName")
                .isEqualTo(Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @Test
    void testAddCustomerWithInvalidEmail() {
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "invalid-email"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("email")
                .isEqualTo(Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE);
    }

    @Test
    void testAddCustomerWithDuplicateEmail() {
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "goober@example.com"
                }
                """;

        // Create the first customer
        mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        // Attempt to create a second customer with the same email
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_EMAIL_DUPLICATE + "goober@example.com");
    }

    @Test
    void testGetAllCustomers() {
        assertThat(mockMvcTester.get().uri("/api/customers"))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetCustomerById() throws Exception {
        // Create a customer
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """;

        // Create a customer
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String customerId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Get customer
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/customers/" + customerId)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("firstName")
                .isEqualTo("Joe");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetNonExistentCustomer() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/customers/999")
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_NOT_FOUND + "999");
    }

    @Test
    void testUpdateCustomer() throws Exception {
        // Create a customer
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "joe@example.com"
                }
                """;

        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String customerId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Update the customer
        String updatedCustomerJson = """
                {
                    "firstName": "Joe Jr.",
                    "lastName": "Mama",
                    "email": "joe@example.com"
                }
                """;

        MvcTestResult updateResult = mockMvcTester.put()
                .uri("/api/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedCustomerJson)
                .exchange();

        assertThat(updateResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("firstName")
                .isEqualTo("Joe Jr.");
    }

    @Test
    void testUpdateCustomerWithDuplicateEmail() throws Exception {
        // Create the first customer
        String customerJson1 = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "goober@example.com"
                }
                """;
        MvcTestResult createResult1 = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson1)
                .exchange();

        assertThat(createResult1).hasStatus(HttpStatus.CREATED);

        String customerId1 = ControllerTestUtils.extractIdFromResponse(createResult1);

        // Create the second customer
        String customerJson2 = """
                {
                    "firstName": "Jane",
                    "lastName": "Mama",
                    "email": "jane@example.com"
                    }
                """;

        MvcTestResult createResult2 = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson2)
                .exchange();

        assertThat(createResult2).hasStatus(HttpStatus.CREATED);

        // Attempt to update the first customer with the second customer's email
        String updatedCustomerJson = """
                {
                    "firstName": "Joe Updated",
                    "lastName": "Mama Updated",
                    "email": "jane@example.com"
                    }
                """;

        MvcTestResult updateResult = mockMvcTester.put()
                .uri("/api/customers/" + customerId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedCustomerJson)
                .exchange();

        assertThat(updateResult)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_EMAIL_DUPLICATE + "jane@example.com");
    }

    @Test
    void testUpdateCustomerWithMissingFields() throws Exception {
        // Create a customer
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "goober@example.com"
                }
                """;
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();
        assertThat(createResult).hasStatus(HttpStatus.CREATED);
        String customerId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Update the customer with null fields
        String updatedCustomerJson = """
                {
                }
                """;

        MvcTestResult updateResult = mockMvcTester.put()
                .uri("/api/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedCustomerJson)
                .exchange();

        assertThat(updateResult)
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(updateResult)
                .bodyJson()
                .extractingPath("firstName")
                .isEqualTo(Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertThat(updateResult)
                .bodyJson()
                .extractingPath("lastName")
                .isEqualTo(Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @ParameterizedTest
    @WithMockUser(roles = "LIBRARIAN")
    @CsvSource({
            "AVAILABLE, borrow, BORROWED",
            "AVAILABLE, reserve, RESERVED"
    })
    void testBorrowReserveCopy(String initialStatus, String action, String expectedFinalStatus) throws Exception {
        // Create a customer
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """;
        MvcTestResult customerResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(customerResult).hasStatus(HttpStatus.CREATED);
        String customerId = ControllerTestUtils.extractIdFromResponse(customerResult);

        // Create a book
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        MvcTestResult bookResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        assertThat(bookResult).hasStatus(HttpStatus.CREATED);

        // Create a copy
        String copyJson = """
                {
                    "book": {"isbn": "%s"},
                    "status": "%s",
                    "customer": TBD
                    }
                """.formatted(bookData.ISBN, initialStatus);

        if (initialStatus.equals("AVAILABLE")) {
            copyJson = copyJson.replace("TBD", "null"); // No customer for available copies
        } else {
            copyJson = copyJson.replace("TBD", """
                    {
                        "id": %s
                    }
                    """.formatted(customerId)); // Associate customer for reserved or borrowed copies
        }

        MvcTestResult copyResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(copyResult).hasStatus(HttpStatus.CREATED);

        // Borrow the copy
        MvcTestResult borrowResult = mockMvcTester.post()
                .uri("/api/customers/" + customerId + "/" + action + "/" + bookData.ISBN)
                .exchange();

        assertThat(borrowResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo(expectedFinalStatus);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testBorrowReserveNonexistentCopy() throws Exception {
        Long customerId = ControllerTestUtils.addCustomer(mockMvcTester, "The", "Goober");

        List<String> customerOperations = List.of("borrow", "reserve");

        for (String operation : customerOperations) {
            assertThat(mockMvcTester.post().uri("/api/customers/" + customerId + "/" + operation + "/9781234567890").exchange())
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("error")
                    .isEqualTo(Messages.COPY_NO_AVAILABLE + "9781234567890");
        }
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteCustomer() throws Exception {
        // Create a customer
        String customerJson = """
                {
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """;

        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String customerId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Delete the customer
        MvcTestResult deleteResult = mockMvcTester.delete()
                .uri("/api/customers/" + customerId)
                .exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NO_CONTENT);

        // Verify the customer is deleted
        MvcTestResult getResult = mockMvcTester.get()
                .uri("/api/customers/" + customerId)
                .exchange();

        assertThat(getResult)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_NOT_FOUND + customerId);
    }

    @Test
    void testDeleteNonexistentCustomer() {
        MvcTestResult result = mockMvcTester.delete()
                .uri("/api/customers/999")
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_NOT_FOUND + "999");
    }
}