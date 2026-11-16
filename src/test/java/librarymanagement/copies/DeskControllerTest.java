package librarymanagement.copies;

import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

class DeskControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    /**
     * 1. Log in the librarian, create a book and two copies of it.
     * 2. Register and log in the customer.
     * 3. Customer reserves the first copy.
     * 4. Librarian checks out the reserved copy, then the unreserved one.
     * 5. Librarian returns the reserved copy.
     * 6. Returning it again fails.
     */
    @Test
    void testCheckoutAndReturn() throws Exception {
        MockHttpSession librarianSession = librarianSession();
        String isbn = createBook(librarianSession);
        int reservedCopyId = createCopy(librarianSession, isbn);
        int availableCopyId = createCopy(librarianSession, isbn);
        CustomerAccount customer = customerAccount("joe@example.com", "Joe", "Mama");
        long customerId = customer.customerId();

        assertThat(reserve(customer.session(), isbn))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("id")
                .isEqualTo(reservedCopyId);

        // Reserved and available copies can both be checked out
        MvcTestResult reservedCheckout = desk(librarianSession, "checkout", reservedCopyId, customerId);
        assertThat(reservedCheckout)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("BORROWED");
        assertThat(reservedCheckout)
                .bodyJson()
                .doesNotHavePath("dueAt");
        assertThat(desk(librarianSession, "checkout", availableCopyId, customerId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("BORROWED");

        assertThat(desk(librarianSession, "return", reservedCopyId, customerId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("AVAILABLE");
        assertThat(desk(librarianSession, "return", reservedCopyId, customerId))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NOT_BORROWED + "AVAILABLE");
    }

    @Test
    void testMarkLost() throws Exception {
        MockHttpSession librarianSession = librarianSession();
        int copyId = createCopy(librarianSession, createBook(librarianSession));

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/desk/mark-lost")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"copyId": %d}
                        """.formatted(copyId))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("status").isEqualTo("LOST");
    }

    private String createBook(MockHttpSession librarianSession) {
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Desk Book", "Desk Author"))
                .hasStatus(HttpStatus.CREATED);
        return isbn;
    }

    private int createCopy(MockHttpSession librarianSession, String isbn) throws Exception {
        MvcTestResult result = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ControllerTestUtils.extractIdFromResponseArray(result);
    }

    private MvcTestResult desk(MockHttpSession librarianSession, String action, int copyId, long customerId) {
        return mockMvcTester.post()
                .uri("/api/desk/" + action)
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"copyId": %d, "customerId": %d}
                        """.formatted(copyId, customerId))
                .exchange();
    }
}
