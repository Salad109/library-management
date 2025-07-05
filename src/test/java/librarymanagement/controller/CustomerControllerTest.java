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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private MockMvcTester mockMvcTester;

    @PostConstruct
    void setUp() {
        mockMvcTester = MockMvcTester.create(mockMvc);
    }

    @Test
    void testGetAllCustomers() {
        assertThat(mockMvcTester.get().uri("/api/customers")).hasStatus(HttpStatus.OK);
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
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/customers")
                .contentType("application/json")
                .content(customerJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // Extract the customer ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String customerId = jsonNode.get("id").asText();

        // Get customer
        assertThat(mockMvcTester.get().uri("/api/customers/" + customerId))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetNonexistingCustomer() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/customers/999")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Customer not found with ID: 999");
    }
}
