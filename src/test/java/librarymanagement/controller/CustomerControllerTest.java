package librarymanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockMvcTester mockMvcTester;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void setUp() {
        mockMvcTester = MockMvcTester.create(mockMvc);
    }

    @Test
    void testAddCustomer() throws Exception {
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
    void testAddCustomerWithMissingFields() throws Exception {
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
                .isEqualTo("First name cannot be blank");
        assertThat(result)
                .bodyJson()
                .extractingPath("lastName")
                .isEqualTo("Last name cannot be blank");
    }

    @Test
    void testAddCustomerWithInvalidEmail() throws Exception {
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
                .isEqualTo("Email must be a valid email address");
    }

    @Test
    void testAddCustomerWithDuplicateEmail() throws Exception {
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
                .isEqualTo("Email already exists: goober@example.com");
    }

    @Test
    void testGetAllCustomers() {
        assertThat(mockMvcTester.get().uri("/api/customers"))
                .hasStatus(HttpStatus.OK);
    }

    @Test
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

        String customerId = extractId(createResult);

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
    void testGetNonExistentCustomer() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/customers/999")
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Customer not found with ID: 999");
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

        String customerId = extractId(createResult);

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

        String customerId1 = extractId(createResult1);

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
                .isEqualTo("Email already exists: jane@example.com");
    }

    @Test
    void testUpdateCustomerWithNullFields() throws Exception {
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
        String customerId = extractId(createResult);

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
                .isEqualTo("First name cannot be blank");
        assertThat(updateResult)
                .bodyJson()
                .extractingPath("lastName")
                .isEqualTo("Last name cannot be blank");
    }

    @Test
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

        String customerId = extractId(createResult);

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
                .isEqualTo("Customer not found with ID: " + customerId);
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
                .isEqualTo("Customer not found with ID: 999");
    }

    // Helper

    private String extractId(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asText();
    }
}