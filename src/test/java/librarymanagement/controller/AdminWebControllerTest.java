package librarymanagement.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "LIBRARIAN")
class AdminWebControllerTest {

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
            "/admin/copies/add",
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
