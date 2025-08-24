package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.utils.ControllerTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeskControllerTest {

    @Autowired
    MockMvcTester mockMvcTester;

    @Test
    void testCheckout() throws Exception {
        // Register a customer
        // Extract customerId
        // Login
        // Reserve 1 copy
        // Extract copyId
        // Logout
        // Register a librarian and login
        // Checkout the reserved copy
        // Checkout an unreserved copy
        // Verify the checkout response

        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractCustomerIdFromRegistration(customerRegistrationResult);

        MvcTestResult customerLoginResult = ControllerTestUtils.login(mockMvcTester, "joe");
        assertThat(customerLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession customerSession = (MockHttpSession) customerLoginResult.getRequest().getSession();
        assertThat(customerSession).isNotNull();

        String reservationJson = """
                {
                    "bookIsbn": "9781234567890"
                }
                """;

        MvcTestResult reservationResult = mockMvcTester.post()
                .uri("/api/reservations")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationJson)
                .exchange();

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);

        int reservedCopyId = ControllerTestUtils.extractIdFromResponse(reservationResult);

        MvcTestResult customerLogoutResult = mockMvcTester.post().uri("/api/logout")
                .session(customerSession)
                .exchange();
        assertThat(customerLogoutResult).hasStatus(HttpStatus.OK);

        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian1");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.login(mockMvcTester, "librarian1");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String reservedCheckoutJson = """
                {
                    "copyId": %d,
                    "customerId": %d
                }
                """.formatted(reservedCopyId, customerId);
        String unreservedCheckoutJson = """
                {
                    "copyId": 2,
                    "customerId": %d
                }
                """.formatted(customerId);

        MvcTestResult reservedCheckoutResult = mockMvcTester.post()
                .uri("/api/desk/checkout")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservedCheckoutJson)
                .exchange();

        assertThat(reservedCheckoutResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .asNumber()
                .isEqualTo(reservedCopyId);

        MvcTestResult unreservedCheckoutResult = mockMvcTester.post()
                .uri("/api/desk/checkout")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unreservedCheckoutJson)
                .exchange();

        assertThat(unreservedCheckoutResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void testReturnCopy() throws Exception {
        // Register a customer
        // Extract customerId
        // Register a librarian and login
        // Checkout a  copy
        // Return the checked out copy
        // Verify the return response

        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "jane", "Jane", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractCustomerIdFromRegistration(customerRegistrationResult);

        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian2");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.login(mockMvcTester, "librarian2");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String checkoutJson = """
                {
                    "copyId": 1,
                    "customerId": %d
                }
                """.formatted(customerId);

        MvcTestResult checkoutResult = mockMvcTester.post()
                .uri("/api/desk/checkout")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutJson)
                .exchange();

        assertThat(checkoutResult).hasStatus(HttpStatus.OK);

        int copyId = ControllerTestUtils.extractIdFromResponse(checkoutResult);

        String returnJson = """
                {
                    "copyId": %d,
                    "customerId": %d
                }
                """.formatted(copyId, customerId);

        MvcTestResult returnResult = mockMvcTester.post()
                .uri("/api/desk/return")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(returnJson)
                .exchange();

        assertThat(returnResult)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .asNumber()
                .isEqualTo(copyId);
    }

    @Test
    void returnNotBorrowedCopy() {
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian3");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.login(mockMvcTester, "librarian3");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String returnJson = """
                {
                    "copyId": 1,
                    "customerId": 1
                }
                """;

        MvcTestResult returnResult = mockMvcTester.post()
                .uri("/api/desk/return")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(returnJson)
                .exchange();

        assertThat(returnResult).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testMarkLost() {
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian3");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.login(mockMvcTester, "librarian3");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        String markLostJson = """
                {
                    "copyId": 1
                }
                """;

        MvcTestResult markLostResult = mockMvcTester.post()
                .uri("/api/desk/mark-lost")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(markLostJson)
                .exchange();

        assertThat(markLostResult)
                .hasStatus(HttpStatus.OK);
        assertThat(markLostResult)
                .bodyJson()
                .extractingPath("id")
                .asNumber()
                .isEqualTo(1);
        assertThat(markLostResult)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("LOST");
    }
}
