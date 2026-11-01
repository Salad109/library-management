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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testMyReservations() {
        MockHttpSession session = customerAccount("jane", "Jane", "Mama").session();

        MvcTestResult result = mockMvcTester.get()
                .uri("/api/reservations/mine")
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("page.totalElements").isEqualTo(0);
    }

    @Test
    void testCreateReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MockHttpSession librarianSession = librarianSession();

        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Reserved Book", "Reserved Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create a customer and make a reservation
        MockHttpSession customerSession = customerAccount("joe", "Joe", "Mama").session();

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn);

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("id")
                .isEqualTo(copyId);
    }

    @Test
    void testReservationLimit() throws Exception {
        MockHttpSession librarianSession = librarianSession();

        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Popular Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, CopyService.MAX_ACTIVE_RESERVATIONS + 1))
                .hasStatus(HttpStatus.CREATED);

        MockHttpSession customerSession = customerAccount("hoarder", "Joe", "Mama").session();

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn);

        int firstCopyId = 0;
        for (int i = 0; i < CopyService.MAX_ACTIVE_RESERVATIONS; i++) {
            MvcTestResult result = mockMvcTester.post()
                    .uri("/api/reservations")
                    .session(customerSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reservationJson)
                    .exchange();
            assertThat(result).hasStatus(HttpStatus.CREATED);
            if (i == 0) {
                firstCopyId = ControllerTestUtils.extractIdFromResponse(result);
            }
        }

        // One over the limit
        assertThat(mockMvcTester.post()
                .uri("/api/reservations")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_RESERVATION_LIMIT + CopyService.MAX_ACTIVE_RESERVATIONS);

        // Cancelling one frees a slot
        assertThat(mockMvcTester.delete()
                .uri("/api/reservations/" + firstCopyId)
                .session(customerSession))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mockMvcTester.post()
                .uri("/api/reservations")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testConcurrentReservationsGetDifferentCopies() throws Exception {
        MockHttpSession librarianSession = librarianSession();

        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Two Copies", "Author Name"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 2))
                .hasStatus(HttpStatus.CREATED);

        // Log both customers in first, so only the reservations race
        MockHttpSession[] sessions = new MockHttpSession[2];
        for (int i = 0; i < 2; i++) {
            sessions[i] = customerAccount("racer" + i, "Racer", "No" + i).session();
        }

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn);

        // Both pick the same first available copy; the loser's retry must pick the other one
        CountDownLatch startLatch = new CountDownLatch(1);
        MvcTestResult[] results = new MvcTestResult[2];
        Thread[] threads = new Thread[2];
        for (int i = 0; i < 2; i++) {
            int index = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    results[index] = mockMvcTester.post()
                            .uri("/api/reservations")
                            .session(sessions[index])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(reservationJson)
                            .exchange();
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
    void testCreateConcurrentReservations() throws Exception {
        // Create a librarian and add a book and a copy
        String isbn = "9783213211230";

        MockHttpSession librarianSession = librarianSession();

        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn);

        // Log both customers in first, so only the reservations race
        MockHttpSession sessionA = customerAccount("customerACon", "Joe", "Mama").session();
        MockHttpSession sessionB = customerAccount("customerBCon", "Jane", "Mama").session();

        // Setup latches to coordinate customer threads
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        MvcTestResult[] results = new MvcTestResult[2];
        Exception[] exceptions = new Exception[2];

        // Customer A reservation thread
        Thread customerAThread = new Thread(() -> {
            try {
                startLatch.await();

                results[0] = mockMvcTester.post()
                        .uri("/api/reservations")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson)
                        .exchange();
            } catch (Exception e) {
                exceptions[0] = e;
            } finally {
                doneLatch.countDown();
            }
        });

        // Customer B reservation thread
        Thread customerBThread = new Thread(() -> {
            try {
                startLatch.await();

                results[1] = mockMvcTester.post()
                        .uri("/api/reservations")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson)
                        .exchange();
            } catch (Exception e) {
                exceptions[1] = e;
            } finally {
                doneLatch.countDown();
            }
        });

        // Start both threads simultaneously
        customerAThread.start();
        customerBThread.start();
        startLatch.countDown();

        // Wait to complete
        doneLatch.await();

        if (exceptions[0] != null) throw exceptions[0];
        if (exceptions[1] != null) throw exceptions[1];

        int successCount = 0;
        int failureCount = 0;

        for (MvcTestResult result : results) {
            int status = result.getResponse().getStatus();

            if (status == HttpStatus.CREATED.value()) {
                successCount++;
                assertThat(result)
                        .bodyJson()
                        .extractingPath("id")
                        .isEqualTo(ControllerTestUtils.extractIdFromResponseArray(copyCreationResult));
            } else if (status == HttpStatus.NOT_FOUND.value() || status == HttpStatus.CONFLICT.value()) {
                // NOT_FOUND when no copies available, CONFLICT on optimistic lock failure
                failureCount++;
            }
        }

        // Exactly one should succeed and one should fail
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
    }

    @Test
    void testCreateInvalidReservations() {
        MockHttpSession session = customerAccount("joe", "Joe", "Mama").session();

        String nullIsbnReservationJson = """
                {
                }
                """;
        String nonExistentIsbn = "9789999999999";
        String nonExistentIsbnReservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(nonExistentIsbn);

        MvcTestResult nullIsbnReservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(nullIsbnReservationJson)
                .exchange();
        MvcTestResult nonExistentIsbnReservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(nonExistentIsbnReservationJson)
                .exchange();

        assertFieldError(nullIsbnReservationResult, "bookIsbn", Messages.BOOK_NULL_ISBN);
        assertThat(nonExistentIsbnReservationResult)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NO_AVAILABLE + nonExistentIsbn);
    }

    @Test
    void testCancelOtherCustomersReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MockHttpSession librarianSession = librarianSession();

        String isbn2 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn2, "Evil Book", "Evil Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn2, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        // Customer A reserves a book
        CustomerAccount customerA = customerAccount("customerA", "Joe", "Mama");
        long customerAId = customerA.customerId();
        MockHttpSession sessionA = customerA.session();

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn2);

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);
        int reservedCopyId = ControllerTestUtils.extractIdFromResponse(reservationResult);

        // Customer B tries to delete Customer A's reservation
        MockHttpSession sessionB = customerAccount("customerB", "Evil Joe", "Mama").session();

        // Invalid delete request by Customer B
        MvcTestResult deleteResult = mockMvcTester.delete()
                .uri("/api/reservations/" + reservedCopyId)
                .session(sessionB)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(deleteResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_WRONG_CUSTOMER + customerAId);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testLibrarianCannotReserve() {
        String isbn3 = TestISBNGenerator.next();
        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn3);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void testCancelReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MockHttpSession librarianSession = librarianSession();

        String isbn4 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn4, "Cancelled Book", "Cancelled Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn4, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        // Create a customer and make a reservation
        MockHttpSession customerSession = customerAccount("joe", "Joe", "Mama").session();

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn4);

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        int reservationCopyId = ControllerTestUtils.extractIdFromResponse(reservationResult);

        MvcTestResult cancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + reservationCopyId)
                .session(customerSession)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(cancelResult).hasStatus(HttpStatus.NO_CONTENT).bodyText().isEmpty();
    }

    @Test
    void testCancelInvalidReservations() throws Exception {
        // Create a librarian and add a book and a copy
        MockHttpSession librarianSession = librarianSession();

        String isbn5 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn5, "Broken Book", "Broken Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn5, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int nonReservedCopyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create a customer
        MockHttpSession session = customerAccount("joe jr", "Joe", "Mama Jr").session();

        // Invalid requests
        int nonExistentReservationCopyId = 999;

        MvcTestResult nonExistentReservationCancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + nonExistentReservationCopyId)
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        MvcTestResult nonReservedReservationCancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + nonReservedCopyId)
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(nonExistentReservationCancelResult).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(nonReservedReservationCancelResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NOT_RESERVED + "AVAILABLE");
    }
}
