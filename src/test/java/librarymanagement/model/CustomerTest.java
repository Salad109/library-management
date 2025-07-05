package librarymanagement.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerTest {

    @Test
    void testCopyRelationshipEdgeCases() {
        Customer customer = new Customer();

        Copy oldCopy = new Copy();
        Copy newCopy = new Copy();

        // Test setting null
        customer.setCopies(null);
        assertThat(customer.getCopies()).isNotNull().isEmpty();

        // Test replacing copies
        customer.setCopies(Set.of(oldCopy));
        customer.setCopies(Set.of(newCopy));

        // Old copy should no longer be associated
        assertThat(oldCopy.getCustomer()).isNull();
        assertThat(newCopy.getCustomer()).isEqualTo(customer);
    }
}
