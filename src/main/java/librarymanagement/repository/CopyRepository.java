package librarymanagement.repository;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {
    Page<Copy> findByBookIsbn(String isbn, Pageable pageable);

    Page<Copy> findByBookIsbnAndStatus(String isbn, CopyStatus status, Pageable pageable);

    long countByBookIsbnAndStatus(String isbn, CopyStatus status);
}
