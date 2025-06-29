package librarymanagement.repository;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {
    @Query("SELECT DISTINCT c FROM Copy c " +
            "LEFT JOIN c.book b " +
            "WHERE (:isbn IS NULL OR b.isbn = :isbn) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Copy> searchCopies(@Param("isbn") String isbn,
                            @Param("status") CopyStatus status,
                            Pageable pageable);

    Page<Copy> findByBookIsbn(String isbn, Pageable pageable);

    Page<Copy> findByBookIsbnAndStatus(String isbn, CopyStatus status, Pageable pageable);

    long countByBookIsbnAndStatus(String isbn, CopyStatus status);
}
