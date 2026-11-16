package librarymanagement.copies;

import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeskControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${library.loans.period-days}")
    private int loanPeriodDays;

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
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        MvcTestResult reservedCheckout = desk(librarianSession, "checkout", reservedCopyId, customerId);
        Instant after = Instant.now();
        assertThat(reservedCheckout)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("BORROWED");
        Duration loanPeriod = Duration.ofDays(loanPeriodDays);
        assertThat(ControllerTestUtils.extractDueAtFromResponse(reservedCheckout))
                .isBetween(before.plus(loanPeriod), after.plus(loanPeriod));
        assertThat(desk(librarianSession, "checkout", availableCopyId, customerId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("BORROWED");

        MvcTestResult returned = desk(librarianSession, "return", reservedCopyId, customerId);
        assertThat(returned)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("AVAILABLE");
        assertThat(returned)
                .bodyJson()
                .doesNotHavePath("dueAt");
        assertThat(desk(librarianSession, "return", reservedCopyId, customerId))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NOT_BORROWED + "AVAILABLE");
    }

    /**
     * 1. Log in the librarian, create a book with two copies and a customer.
     * 2. Check out the first copy, then move its due date into the past.
     * 3. Renewing the overdue loan makes it due a full loan period from now.
     * 4. Renewing again works.
     * 5. Renewing the copy that isn't borrowed fails.
     */
    @Test
    void testRenewLoan() throws Exception {
        MockHttpSession librarianSession = librarianSession();
        String isbn = createBook(librarianSession);
        int borrowedCopyId = createCopy(librarianSession, isbn);
        int availableCopyId = createCopy(librarianSession, isbn);
        long customerId = customerAccount("joe@example.com", "Joe", "Mama").customerId();
        assertThat(desk(librarianSession, "checkout", borrowedCopyId, customerId)).hasStatus(HttpStatus.OK);
        jdbcTemplate.update("UPDATE copies SET due_at = now() - INTERVAL '1 day' WHERE id = ?", borrowedCopyId);

        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        MvcTestResult renewed = desk(librarianSession, "renew", borrowedCopyId, customerId);
        Instant after = Instant.now();

        assertThat(renewed).hasStatus(HttpStatus.OK).bodyJson().extractingPath("renewals").isEqualTo(1);
        Duration loanPeriod = Duration.ofDays(loanPeriodDays);
        assertThat(ControllerTestUtils.extractDueAtFromResponse(renewed))
                .isBetween(before.plus(loanPeriod), after.plus(loanPeriod));
        assertThat(desk(librarianSession, "renew", borrowedCopyId, customerId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("renewals")
                .isEqualTo(2);
        assertThat(desk(librarianSession, "renew", availableCopyId, customerId))
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
