package librarymanagement.repository;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {
    Page<Copy> findByBookIsbn(String isbn, Pageable pageable);

    Optional<Copy> findFirstByBookIsbnAndStatus(String isbn, CopyStatus status);

    Page<Copy> findByCustomerId(Long customerId, Pageable pageable);

    long countByBookIsbnAndStatus(String isbn, CopyStatus status);
}
