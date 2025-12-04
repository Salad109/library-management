package librarymanagement.copies;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {

    @Query("SELECT c FROM Copy c " +
            "LEFT JOIN FETCH c.book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH c.customer " +
            "WHERE c.book.isbn = :isbn AND c.status = :status " +
            "ORDER BY c.id LIMIT 1")
    Optional<Copy> findFirstByBookIsbnAndStatusWithRelations(String isbn, CopyStatus status);

    @Query("SELECT c.id FROM Copy c ORDER BY c.id")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT c FROM Copy c " +
            "LEFT JOIN FETCH c.book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH c.customer " +
            "WHERE c.id IN :ids ORDER BY c.id")
    List<Copy> findByIdsWithAllRelations(List<Long> ids);

    @Query("SELECT c.id FROM Copy c WHERE c.book.isbn = :isbn ORDER BY c.id")
    Page<Long> findIdsByBookIsbn(String isbn, Pageable pageable);

    @Query("SELECT c.id FROM Copy c WHERE c.customer.id = :customerId ORDER BY c.id")
    Page<Long> findIdsByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT c.id FROM Copy c WHERE c.book.title ILIKE CONCAT('%', :title, '%') ORDER BY c.id")
    Page<Long> findIdsByBookTitle(String title, Pageable pageable);

    @Query("SELECT c.id FROM Copy c WHERE c.customer.lastName ILIKE CONCAT('%', :lastName, '%') ORDER BY c.id")
    Page<Long> findIdsByCustomerLastName(String lastName, Pageable pageable);
}
