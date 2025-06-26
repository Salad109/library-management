package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.repository.CopyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    private final CopyRepository copyRepository;

    public CopyService(CopyRepository copyRepository) {
        this.copyRepository = copyRepository;
    }

    public List<Copy> getAllCopies() {
        return copyRepository.findAll();
    }

    public List<Copy> getCopiesByBookIsbn(String isbn) {
        return copyRepository.findByBookIsbn(isbn);
    }

    public List<Copy> getCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.findByBookIsbnAndStatus(isbn, status);
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.countByBookIsbnAndStatus(isbn, status);
    }

    @Transactional
    public Copy addCopy(Copy copy) {
        if (copy.getBook() == null) {
            throw new IllegalArgumentException("Copy must be associated with an existing book");
        }
        return copyRepository.save(copy);
    }

    @Transactional
    public Copy updateCopy(Long id, Copy copy) {
        if (!copyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        copy.setId(id);
        return copyRepository.save(copy);
    }

    @Transactional
    public Copy borrowCopy(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy is not available for borrowing");
        }

        optionalCopy.get().setStatus(CopyStatus.BORROWED);
        return copyRepository.save(optionalCopy.get());
    }

    @Transactional
    public Copy returnCopy(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.BORROWED) {
            throw new IllegalStateException("Copy is not currently borrowed");
        }

        optionalCopy.get().setStatus(CopyStatus.AVAILABLE);
        return copyRepository.save(optionalCopy.get());
    }

    @Transactional
    public Copy markCopyAsLost(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }

        optionalCopy.get().setStatus(CopyStatus.LOST);
        return copyRepository.save(optionalCopy.get());
    }

    @Transactional
    public void deleteCopy(Long id) {
        if (!copyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        copyRepository.deleteById(id);
    }
}
