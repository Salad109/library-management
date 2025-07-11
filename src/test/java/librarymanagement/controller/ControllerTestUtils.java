package librarymanagement.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import librarymanagement.testdata.BookTestData;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ControllerTestUtils {

    /**
     * @return the ID of the newly created customer
     */
    static Long addCustomer(MockMvcTester mockMvcTester, String firstName, String lastName) throws Exception {
        String customerJson = """
                {
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """.formatted(firstName, lastName);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        return Long.parseLong(extractIdFromResponse(result));
    }

    /**
     * @return the ID as a String
     */
    static String extractIdFromResponse(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asText();
    }

    /**
     * @return the JSON string representing the copy
     */
    static String createCopyJson(String isbn, String status) {
        return """
                {
                    "book": {"isbn": "%s"},
                    "status": "%s"
                }
                """.formatted(isbn, status);
    }

    static void addBook(MockMvcTester mockMvcTester, BookTestData.BookData bookData) {
        try {
            mockMvcTester.post()
                    .uri("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookData.JSON)
                    .exchange()
                    .assertThat()
                    .hasStatus(org.springframework.http.HttpStatus.CREATED);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add book: " + bookData.ISBN, e);
        }
    }

    /**
     * @return MvcTestResult containing the POST response of creating a copy
     */
    static MvcTestResult createCopyAndPost(MockMvcTester mockMvcTester, String isbn, String status) {
        String copyJson = ControllerTestUtils.createCopyJson(isbn, status);
        return mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();
    }
}
