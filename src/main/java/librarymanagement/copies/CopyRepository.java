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

    Optional<Copy> findFirstByBookIsbnAndStatusOrderById(String isbn, CopyStatus status);

    boolean existsByBookIsbn(String isbn);

    long countByCustomerIdAndStatus(Long customerId, CopyStatus status);

    // Null filters are ignored. The casts stop Postgres from reading a null string as bytea.
    @Query("""
            SELECT c.id FROM Copy c JOIN c.book b LEFT JOIN c.customer cu
            WHERE (:isbn IS NULL OR b.isbn = :isbn)
            AND (:title IS NULL OR b.title ILIKE CONCAT('%', CAST(:title AS String), '%'))
            AND (:customerId IS NULL OR cu.id = :customerId)
            AND (:customerLastName IS NULL OR cu.lastName ILIKE CONCAT('%', CAST(:customerLastName AS String), '%'))
            ORDER BY c.id
            """)
    Page<Long> findIds(String isbn, String title, Long customerId, String customerLastName, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Copy c " +
            "LEFT JOIN FETCH c.book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH c.customer " +
            "WHERE c.id IN :ids ORDER BY c.id")
    List<Copy> findByIdsWithAllRelations(List<Long> ids);
}
