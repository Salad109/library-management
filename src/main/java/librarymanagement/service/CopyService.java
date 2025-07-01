package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.repository.CopyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CopyService {

    private final CopyRepository copyRepository;

    public CopyService(CopyRepository copyRepository) {
        this.copyRepository = copyRepository;
    }

    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyRepository.findAll(pageable);
    }

    public Page<Copy> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        return copyRepository.findByBookIsbn(isbn, pageable);
    }

    public Page<Copy> getCopiesByBookIsbnAndStatus(String isbn, CopyStatus status, Pageable pageable) {
        return copyRepository.findByBookIsbnAndStatus(isbn, status, pageable);
    }

    public Page<Copy> searchCopies(String isbn, CopyStatus status, Pageable pageable) {
        // If no parameters provided, return all copies
        if (isbn == null && status == null) {
            return copyRepository.findAll(pageable);
        } else {
            return copyRepository.searchCopies(isbn, status, pageable);
        }
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.countByBookIsbnAndStatus(isbn, status);
    }

    @Transactional
    public Copy addCopy(Copy copy) {
        return copyRepository.save(copy);
    }

    @Transactional
    public Copy borrowCopy(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy is not available for borrowing. Current status: " + optionalCopy.get().getStatus());
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
            throw new IllegalStateException("Copy is not currently borrowed. Current status: " + optionalCopy.get().getStatus());
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
    public Copy reserveCopy(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy is not available for reservation. Current status: " + optionalCopy.get().getStatus());
        }

        optionalCopy.get().setStatus(CopyStatus.RESERVED);
        return copyRepository.save(optionalCopy.get());
    }

    @Transactional
    public Copy cancelCopyReservation(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException("Copy is not currently reserved. Current status: " + optionalCopy.get().getStatus());
        }

        optionalCopy.get().setStatus(CopyStatus.AVAILABLE);
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
