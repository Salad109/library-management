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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private CopyService copyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${library.reservations.max-active}")
    private int maxActiveReservations;

    @Value("${library.reservations.hold-days}")
    private int reservationHoldDays;

    @Value("${library.reservations.max-extensions}")
    private int maxReservationExtensions;

    @Test
    void testCreateReservation() throws Exception {
        String isbn = createBookWithCopies(1);
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();

        MvcTestResult result = reserve(session, isbn);

        assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("status").isEqualTo("RESERVED");
        assertThat(mockMvcTester.get().uri("/api/reservations/mine").session(session))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content[0].id")
                .isEqualTo(ControllerTestUtils.extractIdFromResponse(result));
    }

    /**
     * 1. Create a book with one copy more than the reservation limit.
     * 2. Customer reserves up to the limit.
     * 3. One more reservation is rejected.
     * 4. Customer cancels one, which frees a slot for another.
     */
    @Test
    void testReservationLimit() throws Exception {
        String isbn = createBookWithCopies(maxActiveReservations + 1);
        MockHttpSession session = customerAccount("hoarder@example.com", "Joe", "Mama").session();

        int firstCopyId = ControllerTestUtils.extractIdFromResponse(reserve(session, isbn));
        for (int i = 1; i < maxActiveReservations; i++) {
            assertThat(reserve(session, isbn)).hasStatus(HttpStatus.CREATED);
        }

        assertThat(reserve(session, isbn))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_RESERVATION_LIMIT + maxActiveReservations);

        // Cancelling one frees a slot
        assertThat(mockMvcTester.delete().uri("/api/reservations/" + firstCopyId).session(session))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(reserve(session, isbn)).hasStatus(HttpStatus.CREATED);
    }

    /**
     * 1. Create a book with two copies and reserve both.
     * 2. Move the first reservation's deadline into the past.
     * 3. Run the expiry.
     * 4. The book shows one available copy again.
     */
    @Test
    void testReservationExpires() throws Exception {
        String isbn = createBookWithCopies(2);
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();

        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        MvcTestResult first = reserve(session, isbn);
        Instant after = Instant.now();
        Duration hold = Duration.ofDays(reservationHoldDays);
        assertThat(ControllerTestUtils.extractDueAtFromResponse(first)).isBetween(before.plus(hold), after.plus(hold));
        int expiredCopyId = ControllerTestUtils.extractIdFromResponse(first);
        int keptCopyId = ControllerTestUtils.extractIdFromResponse(reserve(session, isbn));

        jdbcTemplate.update("UPDATE copies SET due_at = now() - INTERVAL '1 minute' WHERE id = ?", expiredCopyId);
        copyService.expireReservations();

        assertThat(mockMvcTester.get().uri("/api/reservations/mine").session(session))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content[*].id")
                .asArray()
                .containsExactly(keptCopyId);
        assertThat(mockMvcTester.get().uri("/api/books/" + isbn))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("availableCopies")
                .isEqualTo(1);
    }

    @Test
    void testExtendReservation() throws Exception {
        String isbn = createBookWithCopies(1);
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();
        MvcTestResult reservation = reserve(session, isbn);
        int copyId = ControllerTestUtils.extractIdFromResponse(reservation);
        assertThat(reservation).bodyJson().extractingPath("renewals").isEqualTo(0);

        MvcTestResult extended = mockMvcTester.post().uri("/api/reservations/" + copyId + "/extend").session(session).exchange();

        assertThat(extended).hasStatus(HttpStatus.OK).bodyJson().extractingPath("renewals").isEqualTo(1);
        Instant reservedUntil = ControllerTestUtils.extractDueAtFromResponse(reservation);
        assertThat(ControllerTestUtils.extractDueAtFromResponse(extended))
                .isEqualTo(reservedUntil.plus(Duration.ofDays(reservationHoldDays)));
        assertThat(mockMvcTester.post().uri("/api/reservations/" + copyId + "/extend").session(session))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_EXTENSION_LIMIT + maxReservationExtensions);
    }

    /**
     * 1. Create a book with two copies.
     * 2. Register and log in two customers.
     * 3. Both reserve at the same moment, so both pick the same first copy.
     * 4. The loser's retry takes the other copy. Both succeed with different copies.
     */
    @Test
    void testConcurrentReservationsGetDifferentCopies() throws Exception {
        String isbn = createBookWithCopies(2);

        // Log both customers in first, so only the reservations race
        MockHttpSession[] sessions = new MockHttpSession[2];
        for (int i = 0; i < 2; i++) {
            sessions[i] = customerAccount("racer" + i + "@example.com", "Racer", "No" + i).session();
        }

        // Both pick the same first available copy; the loser's retry must pick the other one
        CountDownLatch startLatch = new CountDownLatch(1);
        MvcTestResult[] results = new MvcTestResult[2];
        Thread[] threads = new Thread[2];
        for (int i = 0; i < 2; i++) {
            int index = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    results[index] = reserve(sessions[index], isbn);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        startLatch.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(results[0]).hasStatus(HttpStatus.CREATED);
        assertThat(results[1]).hasStatus(HttpStatus.CREATED);
        assertThat(ControllerTestUtils.extractIdFromResponse(results[0]))
                .isNotEqualTo(ControllerTestUtils.extractIdFromResponse(results[1]));
    }

    @Test
    void testReserveWithoutAvailableCopy() {
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();
        String isbn = "9789999999999";

        assertThat(reserve(session, isbn))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NO_AVAILABLE + isbn);
    }

    @Test
    void testCancelOtherCustomersReservation() throws Exception {
        String isbn = createBookWithCopies(1);
        CustomerAccount owner = customerAccount("owner@example.com", "Joe", "Mama");
        int copyId = ControllerTestUtils.extractIdFromResponse(reserve(owner.session(), isbn));
        MockHttpSession otherSession = customerAccount("other@example.com", "Evil Joe", "Mama").session();

        assertThat(mockMvcTester.delete().uri("/api/reservations/" + copyId).session(otherSession))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_WRONG_CUSTOMER + owner.customerId());
    }

    @Test
    void testCancelInvalidReservations() throws Exception {
        MockHttpSession librarianSession = librarianSession();
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        int availableCopyId = ControllerTestUtils.extractIdFromResponseArray(
                DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1));
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();

        assertThat(mockMvcTester.delete().uri("/api/reservations/999").session(session))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mockMvcTester.delete().uri("/api/reservations/" + availableCopyId).session(session))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NOT_RESERVED + "AVAILABLE");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testLibrarianCannotReserve() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookIsbn": "%s"}
                        """.formatted(TestISBNGenerator.next()))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    private String createBookWithCopies(int copies) {
        MockHttpSession librarianSession = librarianSession();
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, copies))
                .hasStatus(HttpStatus.CREATED);
        return isbn;
    }
}
