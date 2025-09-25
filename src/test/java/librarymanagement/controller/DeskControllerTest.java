package librarymanagement.controller;

import jakarta.transaction.Transactional;
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

    /**
     * 1. Register and login the librarian.
     * 2. Create a book and two copies of it.
     * 3. Register and login the customer.
     * 4. Customer reserves one copy.
     * 5. Librarian checks out the reserved copy to the customer.
     * 6. Librarian checks out the unreserved copy to the customer.
     */
    @Test
    void testCheckout() throws Exception {
        // Register and login the librarian
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian1");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian1");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        // Create book
        String isbn = TestISBNGenerator.next();
        MvcTestResult bookCreationResult = DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Checkout Book", "Checkout Author");
        assertThat(bookCreationResult).hasStatus(HttpStatus.CREATED);

        // Create a copy to be reserved and checked out
        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int reservedCopyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create another copy for unreserved checkout
        MvcTestResult secondCopyResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(secondCopyResult).hasStatus(HttpStatus.CREATED);

        int unreservedCopyId = ControllerTestUtils.extractIdFromResponseArray(secondCopyResult);

        // Register and login the customer
        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "joe", "Joe", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractCustomerIdFromRegistration(customerRegistrationResult);

        MvcTestResult customerLoginResult = ControllerTestUtils.loginCustomer(mockMvcTester, "joe");
        assertThat(customerLoginResult).hasStatus(HttpStatus.OK);

        MockHttpSession customerSession = (MockHttpSession) customerLoginResult.getRequest().getSession();
        assertThat(customerSession).isNotNull();

        // Customer reserves a copy
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

        assertThat(reservationResult).hasStatus(HttpStatus.CREATED);

        // Customer logs out
        MvcTestResult customerLogoutResult = mockMvcTester.post().uri("/api/logout")
                .session(customerSession)
                .exchange();
        assertThat(customerLogoutResult).hasStatus(HttpStatus.OK);

        // Librarian checks out the reserved copy
        String reservedCheckoutJson = """
                {
                    "copyId": %d,
                    "customerId": %d
                }
                """.formatted(reservedCopyId, customerId);

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

        // Librarian checks out an unreserved copy
        String unreservedCheckoutJson = """
                {
                    "copyId": %d,
                    "customerId": %d
                }
                """.formatted(unreservedCopyId, customerId);

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
                .isEqualTo(unreservedCopyId);
    }

    /**
     * 1. Register and login the librarian.
     * 2. Create a book and a copy of it.
     * 3. Register and login the customer.
     * 4. Librarian checks out the copy to the customer.
     * 5. Customer returns the copy.
     */
    @Test
    void testReturnCopy() throws Exception {
        // Register and login the librarian
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian2");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian2");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        // Create book and copy
        String isbn = TestISBNGenerator.next();
        MvcTestResult bookCreationResult = DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Return Book", "Return Author");
        assertThat(bookCreationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Create customer
        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "jane", "Jane", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractCustomerIdFromRegistration(customerRegistrationResult);

        // Checkout the copy first
        String checkoutJson = """
                {
                    "copyId": %d,
                    "customerId": %d
                }
                """.formatted(copyId, customerId);

        MvcTestResult checkoutResult = mockMvcTester.post()
                .uri("/api/desk/checkout")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutJson)
                .exchange();

        assertThat(checkoutResult).hasStatus(HttpStatus.OK);

        // Return the copy
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

    /**
     * 1. Register and login the librarian.
     * 2. Create a book and a copy of it.
     * 3. Register the customer.
     * 4. Try to return the copy that was never borrowed.
     */
    @Test
    void testReturnNotBorrowedCopy() throws Exception {
        // Register and login the librarian
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian3");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian3");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        // Create book and copy
        String isbn = TestISBNGenerator.next();
        MvcTestResult bookCreationResult = DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Not Borrowed Book", "Not Borrowed Author");
        assertThat(bookCreationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Register customer
        MvcTestResult customerRegistrationResult = ControllerTestUtils.registerCustomer(mockMvcTester, "baby joe", "Joe Jr", "Mama");
        assertThat(customerRegistrationResult).hasStatus(HttpStatus.CREATED);

        int customerId = ControllerTestUtils.extractCustomerIdFromRegistration(customerRegistrationResult);

        // Try to return copy that was never borrowed
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

        assertThat(returnResult).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testMarkLost() throws Exception {
        // Register and login the librarian
        MvcTestResult librarianRegistrationResult = ControllerTestUtils.registerLibrarian(mockMvcTester, "librarian4");
        assertThat(librarianRegistrationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult librarianLoginResult = ControllerTestUtils.loginLibrarian(mockMvcTester, "librarian4");
        assertThat(librarianLoginResult).hasStatus(HttpStatus.FOUND);

        MockHttpSession librarianSession = (MockHttpSession) librarianLoginResult.getRequest().getSession();
        assertThat(librarianSession).isNotNull();

        // Create book and copy
        String isbn = TestISBNGenerator.next();
        MvcTestResult bookCreationResult = DataBuilder.createTestBook(mockMvcTester, librarianSession, isbn, "Lost Book", "Lost Author");
        assertThat(bookCreationResult).hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, librarianSession, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);

        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        // Mark copy as lost
        String markLostJson = """
                {
                    "copyId": %d
                }
                """.formatted(copyId);

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
                .isEqualTo(copyId);
        assertThat(markLostResult)
                .bodyJson()
                .extractingPath("status")
                .isEqualTo("LOST");
    }
}