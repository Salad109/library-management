package librarymanagement.web;

import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "LIBRARIAN")
class AdminWebControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @ParameterizedTest
    @ValueSource(strings = {
            "/login",
            "/admin",
            "/admin/books/browse",
            "/admin/books/add",
            "/admin/copies/browse",
            "/admin/customers/browse",
            "/admin/customers/add",
            "/admin/librarians/add",
    })
    void testPageLoads(String url) {
        mockMvcTester.get().uri(url)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void testBookBrowseShowsBooks() {
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "Browse Title", "Browse Author"))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/admin/books/browse"))
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains("Browse Title", "Browse Author");
    }

    @Test
    void testCustomerBrowseShowsCustomers() {
        assertThat(DataBuilder.createTestCustomer(mockMvcTester, "Browse", "Customer", "browse@example.com"))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/admin/customers/browse"))
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains("Browse Customer", "browse@example.com", "data-name=\"Browse Customer\"",
                        "function generateActivationCode");
    }
}
