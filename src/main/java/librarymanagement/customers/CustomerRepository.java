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

    @Query("""
            SELECT new librarymanagement.customers.CustomerResponse(c.id, c.firstName, c.lastName, u.email)
            FROM Customer c LEFT JOIN User u ON u.customer = c
            ORDER BY c.lastName, c.firstName
            """)
    Page<CustomerResponse> findAllResponses(Pageable pageable);

    @Query("""
            SELECT new librarymanagement.customers.CustomerResponse(c.id, c.firstName, c.lastName, u.email)
            FROM Customer c LEFT JOIN User u ON u.customer = c
            WHERE c.lastName ILIKE CONCAT('%', :lastName, '%')
            ORDER BY c.lastName, c.firstName
            """)
    Page<CustomerResponse> findResponsesByLastName(String lastName, Pageable pageable);
}
