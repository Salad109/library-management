package librarymanagement.utils;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Methods without a session rely on the test's @WithMockUser.
 */
public final class DataBuilder {

    private DataBuilder() {
    }

    public static MvcTestResult createTestBook(MockMvcTester mockMvcTester, String isbn, String title, String authorName) {
        return createTestBook(mockMvcTester, null, isbn, title, authorName);
    }

    public static MvcTestResult createTestBook(MockMvcTester mockMvcTester,
                                               MockHttpSession librarianSession,
                                               String isbn,
                                               String title,
                                               String authorName) {
        String json = """
                {"isbn": "%s", "title": "%s", "publicationYear": 2024, "authorNames": ["%s"]}
                """.formatted(isbn, title, authorName);
        return post(mockMvcTester, librarianSession, "/api/admin/books", json);
    }

    public static MvcTestResult createTestCopy(MockMvcTester mockMvcTester, String isbn, int quantity) {
        return createTestCopy(mockMvcTester, null, isbn, quantity);
    }

    public static MvcTestResult createTestCopy(MockMvcTester mockMvcTester,
                                               MockHttpSession librarianSession,
                                               String isbn,
                                               int quantity) {
        String json = """
                {"bookIsbn": "%s", "quantity": %d}
                """.formatted(isbn, quantity);
        return post(mockMvcTester, librarianSession, "/api/admin/copies", json);
    }

    public static MvcTestResult createTestCustomer(MockMvcTester mockMvcTester, String firstName, String lastName) {
        String json = """
                {"firstName": "%s", "lastName": "%s"}
                """.formatted(firstName, lastName);
        return post(mockMvcTester, null, "/api/admin/customers", json);
    }

    private static MvcTestResult post(MockMvcTester mockMvcTester, MockHttpSession session, String uri, String json) {
        MockMvcTester.MockMvcRequestBuilder request = mockMvcTester.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (session != null) {
            request.session(session);
        }
        return request.exchange();
    }
}
