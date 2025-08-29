package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReservationControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
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

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("numberOfElements").isEqualTo(0);
    }

    @Test
    void testCreateReservation() {
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        String reservationJson = """
                {
                    "bookIsbn": "9781234567890"
                }
                """;

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);
    }

    @Test
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
    void testCancelOtherCustomersReservation() throws Exception {
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
                    "bookIsbn": "9799876543210"
                }
                """;

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);
        int reservationId = ControllerTestUtils.extractIdFromResponse(reservationResult);

        // Customer B tries to delete Customer A's reservation
        MvcTestResult registrationResultB = ControllerTestUtils.registerCustomer(mockMvcTester, "customerB", "Evil Joe", "Mama");
        assertThat(registrationResultB).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResultB = ControllerTestUtils.loginCustomer(mockMvcTester, "customerB");
        assertThat(loginResultB).hasStatus(HttpStatus.OK);

        MockHttpSession sessionB = (MockHttpSession) loginResultB.getRequest().getSession();
        assertThat(sessionB).isNotNull();

        // Invalid delete request by Customer B
        MvcTestResult deleteResult = mockMvcTester.delete()
                .uri("/api/reservations/" + reservationId)
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
    @WithMockUser(roles = "LIBRARIAN")
    void testLibrarianCannotReserve() {
        String reservationJson = """
                {
                    "bookIsbn": "9781234567890"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void testCancelReservation() throws Exception {
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe jr", "Joe", "Mama Jr");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe jr");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        String reservationJson = """
                {
                    "bookIsbn": "9781234567890"
                }
                """;

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);

        int reservationId = ControllerTestUtils.extractIdFromResponse(reservationResult);

        MvcTestResult cancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + reservationId)
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(cancelResult).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void testCancelInvalidReservations() {
        MvcTestResult registrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe jr", "Joe", "Mama Jr");
        assertThat(registrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe jr");
        assertThat(loginResult).hasStatus(HttpStatus.OK);

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        int nonExistentReservationId = 999;
        int nonReservedReservationId = 1;

        MvcTestResult nonExistentReservationCancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + nonExistentReservationId)
                .session(session)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
        MvcTestResult nonReservedReservationCancelResult = mockMvcTester.delete()
                .uri("/api/reservations/" + nonReservedReservationId)
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
