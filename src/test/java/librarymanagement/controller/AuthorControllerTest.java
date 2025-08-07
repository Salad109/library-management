package librarymanagement.controller;

import librarymanagement.constants.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testGetAuthor() {
        String authorName = "George Orwell";

        MvcTestResult result = mockMvcTester.get().uri("/api/authors/" + authorName).exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("name")
                .isEqualTo(authorName);
    }

    @Test
    void testGetNonExistentAuthor() {
        String nonExistentAuthorName = "Sir Goober";

        MvcTestResult result = mockMvcTester.get().uri("/api/authors/" + nonExistentAuthorName).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.AUTHOR_NOT_FOUND + nonExistentAuthorName);
    }
}
