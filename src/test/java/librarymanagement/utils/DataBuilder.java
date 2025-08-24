package librarymanagement.utils;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public class DataBuilder {

    public static MvcTestResult createTestBook(MockMvcTester mockMvcTester,
                                               String isbn,
                                               String title,
                                               String authorName) {
        String bookJson = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "publicationYear": 2024,
                    "authorNames": ["%s"]
                }
                """.formatted(isbn, title, authorName);

        return mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson)
                .exchange();
    }

    public static MvcTestResult createTestCopy(MockMvcTester mockMvcTester,
                                               String isbn,
                                               int quantity) {
        String copyJson = """
                {
                    "bookIsbn": "%s",
                    "quantity": %d
                }
                """.formatted(isbn, quantity);

        return mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();
    }

    public static MvcTestResult createTestCustomer(MockMvcTester mockMvcTester,
                                                   String firstName,
                                                   String lastName,
                                                   String email) {
        String customerJson = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s"
                }
                """.formatted(firstName, lastName, email);

        return mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();
    }
}
