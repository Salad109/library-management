package librarymanagement.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public final class ControllerTestUtils {
    private ControllerTestUtils() {
    }

    public static MvcTestResult registerCustomer(MockMvcTester mockMvcTester, String username, String firstName, String lastName) {
        String customerJson = """
                {
                    "username": "%s",
                    "password": "password123",
                    "role": "ROLE_CUSTOMER",
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """.formatted(username, firstName, lastName);

        return mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();
    }

    public static MvcTestResult registerLibrarian(MockMvcTester mockMvcTester, String username) {
        String librarianJson = """
                {
                    "username": "%s",
                    "password": "password123",
                    "role": "ROLE_LIBRARIAN"
                }
                """.formatted(username);

        return mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(librarianJson)
                .exchange();
    }

    public static MvcTestResult loginCustomer(MockMvcTester mockMvcTester, String username) {
        return mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=" + username + "&password=password123")
                .exchange();
    }

    public static MvcTestResult loginLibrarian(MockMvcTester mockMvcTester, String username) {
        return mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=" + username + "&password=password123")
                .exchange();
    }

    public static int extractIdFromResponse(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asInt();
    }

    public static int extractIdFromResponseArray(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get(0).get("id").asInt();
    }

    public static int extractCustomerIdFromRegistration(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("customer").get("id").asInt();
    }
}
