package librarymanagement.repository;

import librarymanagement.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT c.id FROM Customer c ORDER BY c.lastName, c.firstName")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT c FROM Customer c " +
            "WHERE c.id IN :ids ORDER BY c.lastName, c.firstName")
    List<Customer> findByIds(@Param("ids") List<Long> ids);

    @Query("SELECT c.id FROM Customer c WHERE c.lastName ILIKE CONCAT('%', :lastName, '%') ORDER BY c.lastName, c.firstName")
    Page<Long> findIdsByLastName(@Param("lastName") String lastName, Pageable pageable);
}
