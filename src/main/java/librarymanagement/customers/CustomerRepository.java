package librarymanagement.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByActivationCodeHash(String activationCodeHash);

    // Null lastName matches everyone. The cast stops Postgres from reading a null string as bytea.
    @Query("""
            SELECT new librarymanagement.customers.CustomerResponse(c.id, c.firstName, c.lastName, u.email)
            FROM Customer c LEFT JOIN User u ON u.customer = c
            WHERE :lastName IS NULL OR c.lastName ILIKE CONCAT('%', CAST(:lastName AS String), '%')
            """)
    Page<CustomerResponse> findResponses(String lastName, Pageable pageable);
}
