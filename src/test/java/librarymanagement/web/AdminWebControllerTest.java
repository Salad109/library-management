package librarymanagement.web;

import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WithMockUser(roles = "LIBRARIAN")
class AdminWebControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @ParameterizedTest
    @ValueSource(strings = {
            "/register",
            "/login",
            "/admin",
            "/admin/books/browse",
            "/admin/books/add",
            "/admin/copies/browse",
            "/admin/customers/browse",
            "/admin/customers/add",
    })
    void testPageLoads(String url) {
        mockMvcTester.get().uri(url)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK);
    }
}
