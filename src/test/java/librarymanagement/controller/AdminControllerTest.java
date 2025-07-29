package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.BookTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    // BOOK MANAGEMENT TESTS

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook1.JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo(BookTestData.TestBook1.TITLE);
        assertThat(result).bodyJson().extractingPath("publicationYear").isEqualTo(BookTestData.TestBook1.PUBLICATION_YEAR);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook1.AUTHOR_NAME);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateDuplicateBook() {
        // Create a book
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook1.JSON)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // Create it again
        MvcTestResult duplicateResult = mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook1.JSON)
                .exchange();

        assertThat(duplicateResult).hasStatus(HttpStatus.CONFLICT);
        assertThat(duplicateResult).bodyJson().extractingPath("error").isEqualTo(Messages.BOOK_DUPLICATE + BookTestData.TestBook1.ISBN);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateBook() {
        // Verify initial state
        MvcTestResult initialResult = mockMvcTester.get()
                .uri("/api/books/123456789X")
                .exchange();

        assertThat(initialResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("title")
                .isEqualTo("The Little Prince");

        // Update the book
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/books/123456789X")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook2.JSON)
                .exchange();

        // Verify the update
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo(BookTestData.TestBook2.TITLE);
        assertThat(result).bodyJson().extractingPath("publicationYear").isEqualTo(BookTestData.TestBook2.PUBLICATION_YEAR);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook2.AUTHOR_NAME);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateNonExistentBook() {
        String isbn = "9784567891230"; // Nonexistent ISBN
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/books/" + isbn)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook2.JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteBook() {
        // Create a book to delete
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook1.JSON)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // Verify the book exists
        MvcTestResult initialResult = mockMvcTester.get()
                .uri("/api/books/" + BookTestData.TestBook1.ISBN)
                .exchange();

        assertThat(initialResult).hasStatus(HttpStatus.OK);

        // Delete the book
        MvcTestResult deleteResult = mockMvcTester.delete()
                .uri("/api/admin/books/" + BookTestData.TestBook1.ISBN)
                .exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NO_CONTENT);

        // Verify the book no longer exists
        MvcTestResult afterDeleteResult = mockMvcTester.get()
                .uri("/api/books/" + BookTestData.TestBook1.ISBN)
                .exchange();

        assertThat(afterDeleteResult).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteNonExistentBook() {
        String isbn = "9784567891230"; // Nonexistent ISBN
        MvcTestResult result = mockMvcTester.delete()
                .uri("/api/admin/books/" + isbn)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }

    // COPY MANAGEMENT TESTS

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetAllCopies() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("content").isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetCopyById() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies/1")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("id").isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetNonexistentCopyById() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies/9999")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.COPY_NOT_FOUND + "9999");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetCopiesByBookIsbn() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies/book/9781234567890")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("content").isNotEmpty();
    }


    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateCopies() {
        String requestJson = """
                {
                    "bookIsbn": "123456789X",
                    "quantity": 5
                }
                """;

        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // Verify the number of copies created
        MvcTestResult getResult = mockMvcTester.get()
                .uri("/api/admin/copies/book/123456789X")
                .exchange();

        assertThat(getResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("numberOfElements")
                .isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateCopiesInvalidParameters() {
        String tooManyCopiesBadIsbnRequestJson = """
                {
                    "bookIsbn": "9781234567890",
                    "quantity": 9999999
                }
                """;
        String tooLittleCopiesRequestJson = """
                {
                    "bookIsbn": "123456789X",
                    "quantity": 0
                }
                """;
        String invalidIsbnRequestJson = """
                {
                    "bookIsbn": "invalid-isbn"
                }
                """;

        MvcTestResult manyResult = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tooManyCopiesBadIsbnRequestJson)
                .exchange();
        MvcTestResult negativeResult = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tooLittleCopiesRequestJson)
                .exchange();
        MvcTestResult invalidIsbnResult = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidIsbnRequestJson)
                .exchange();

        assertThat(manyResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(manyResult).bodyJson().extractingPath("quantity").isEqualTo(Messages.COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE);

        assertThat(negativeResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(negativeResult).bodyJson().extractingPath("quantity").isEqualTo(Messages.COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE);

        assertThat(invalidIsbnResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalidIsbnResult).bodyJson().extractingPath("bookIsbn").isEqualTo(Messages.BOOK_ISBN_VALIDATION_MESSAGE);
    }

    // CUSTOMER MANAGEMENT TESTS

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetAllCustomers() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/customers")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("content").isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetCustomerById() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/customers/1")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo("Joe");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetNonExistentCustomerById() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/customers/9999")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.CUSTOMER_NOT_FOUND + "9999");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateCustomer() {
        String updateJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Mama",
                    "email": "janemama@example.com"
                    }
                """;

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo("Jane");
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo("Mama");
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("janemama@example.com");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateCustomerMissingFields() {
        String updateJson = """
                {
                    "email": "goober@example.com"
                    }
                """;

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo(Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo(Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateCustomerDuplicateEmail() {
        String updateJson = """
                {
                    "firstName": "The",
                    "lastName": "Goober",
                    "email": "joemama@example.com"
                    }
                """;

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/customers/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.CUSTOMER_EMAIL_DUPLICATE + "joemama@example.com");
    }
}