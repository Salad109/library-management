package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    @Transactional
    void testMyReservations() {
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "jane", "Jane", "Mama");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "jane");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        MvcTestResult result = mockMvcTester.get()
                .uri("/api/reservations/mine")
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("page.totalElements").isEqualTo(0);
    }

    @Test
    @Transactional
    void testCreateReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian1");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian1");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create a customer and make a reservation
        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult customerLoginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe");
        assertThat(customerLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession customerSession = (MockHttpSession) customerLoginResult.getRequest().getSession();
        assertThat(customerSession).isNotNull();

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
    void testCreateConcurrentReservations() throws Exception {
        // Create a librarian and add a book and a copy
        String isbn = "9783213211230";

        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarianCon");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarianCon");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        String reservationJson = """
                {
                    "bookIsbn": "%s"
                }
                """.formatted(isbn);

        // Setup latches to coordinate customer threads
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        MvcTestResult[] results = new MvcTestResult[2];
        Exception[] exceptions = new Exception[2];

        // Customer A reservation thread
        Thread customerAThread = new Thread(() -> {
            try {
                startLatch.await();

                // Register and login
                MvcTestResult regResult = ControllerTestUtils.registerCustomer(mockMvcTester, "customerACon", "Joe", "Mama");
                if (regResult.getResponse().getStatus() != HttpStatus.CREATED.value()) {
                    exceptions[0] = new RuntimeException("Failed to register customerA");
                    return;
                }

                MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "customerACon");
                if (loginResult.getResponse().getStatus() != HttpStatus.OK.value()) {
                    exceptions[0] = new RuntimeException("Failed to login customerA");
                    return;
                }

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

                results[0] = mockMvcTester.post()
                        .uri("/api/reservations")
                        .session(session)
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

                // Register and login customer
                MvcTestResult regResult = ControllerTestUtils.registerCustomer(mockMvcTester, "customerBCon", "Jane", "Mama");
                if (regResult.getResponse().getStatus() != HttpStatus.CREATED.value()) {
                    exceptions[1] = new RuntimeException("Failed to register customerB");
                    return;
                }

                MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "customerBCon");
                if (loginResult.getResponse().getStatus() != HttpStatus.OK.value()) {
                    exceptions[1] = new RuntimeException("Failed to login customerB");
                    return;
                }

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

                results[1] = mockMvcTester.post()
                        .uri("/api/reservations")
                        .session(session)
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
            if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
                successCount++;
                assertThat(result)
                        .bodyJson()
                        .extractingPath("id")
                        .isEqualTo(ControllerTestUtils.extractIdFromResponseArray(copyCreationResult));
            } else if (result.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                failureCount++;
            }
        }

        // Exactly one should succeed and fail
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
    }

    @Test
    @Transactional
    void testCreateInvalidReservations() {
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

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

        assertThat(nullIsbnReservationResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("bookIsbn")
                .isEqualTo(Messages.BOOK_NULL_ISBN);
        assertThat(nonExistentIsbnReservationResult)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.COPY_NO_AVAILABLE + nonExistentIsbn);
    }

    @Test
    @Transactional
    void testCancelOtherCustomersReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String isbn2 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn2, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn2, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        // Customer A reserves a book
        MvcTestResult registrationResultA = ControllerTestUtils.registerCustomer(mockMvcTester, "customerA", "Joe", "Mama");
        assertThat(registrationResultA).hasStatus(HttpStatus.CREATED);

        int customerAId = ControllerTestUtils.extractCustomerIdFromRegistration(registrationResultA);

        MvcTestResult loginResultA = ControllerTestUtils.loginCustomer(mockMvcTester, "customerA");
        assertThat(loginResultA).hasStatus(HttpStatus.OK);

        MockHttpSession sessionA = (MockHttpSession) loginResultA.getRequest().getSession();
        assertThat(sessionA).isNotNull();

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
        MvcTestResult registrationResultB = ControllerTestUtils.registerCustomer(mockMvcTester, "customerB", "Evil Joe", "Mama");
        assertThat(registrationResultB).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResultB = ControllerTestUtils.loginCustomer(mockMvcTester, "customerB");
        assertThat(loginResultB).hasStatus(HttpStatus.OK);

        MockHttpSession sessionB = (MockHttpSession) loginResultB.getRequest().getSession();
        assertThat(sessionB).isNotNull();

        // Invalid delete request by Customer B
        MvcTestResult deleteResult = mockMvcTester.delete()
                .uri("/api/reservations/" + reservedCopyId)
                .session(sessionB)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(deleteResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.COPY_WRONG_CUSTOMER + customerAId);
    }

    @Test
    @Transactional
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
    @Transactional
    void testCancelReservation() throws Exception {
        // Create a librarian and add a book and a copy
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String isbn4 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn4, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn4, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        // Create a customer and make a reservation
        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult customerLoginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe");
        assertThat(customerLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession customerSession = (MockHttpSession) customerLoginResult.getRequest().getSession();
        assertThat(customerSession).isNotNull();

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

        assertThat(cancelResult).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @Transactional
    void testCancelInvalidReservations() throws Exception {
        // Create a librarian and add a book and a copy
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String isbn5 = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn5, "Test Book", "Author Name"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn5, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int nonReservedCopyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create a customer
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe jr", "Joe", "Mama Jr");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe jr");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

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
                .extractingPath("error")
                .isEqualTo(Messages.COPY_NOT_RESERVED + "AVAILABLE");
    }
}
