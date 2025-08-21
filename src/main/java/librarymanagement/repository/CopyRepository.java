package librarymanagement.repository;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {

    Optional<Copy> findFirstByBookIsbnAndStatus(String isbn, CopyStatus status);

    long countByBookIsbnAndStatus(String isbn, CopyStatus status);

    @Query("SELECT c.id FROM Copy c ORDER BY c.id")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT c FROM Copy c " +
            "LEFT JOIN FETCH c.book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH c.customer " +
            "WHERE c.id IN :ids ORDER BY c.id")
    List<Copy> findByIdsWithAllRelations(@Param("ids") List<Long> ids);

    @Query("SELECT c.id FROM Copy c WHERE c.book.isbn = :isbn ORDER BY c.id")
    Page<Long> findIdsByBookIsbn(@Param("isbn") String isbn, Pageable pageable);

    @Query("SELECT c.id FROM Copy c WHERE c.customer.id = :customerId ORDER BY c.id")
    Page<Long> findIdsByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
}
