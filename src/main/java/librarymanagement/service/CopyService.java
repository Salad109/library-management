package librarymanagement.service;

import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.repository.CopyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public Copy addCopy(Copy copy) {
        return copyRepository.save(copy);
    }

    public void deleteCopy(Long id) {
        copyRepository.deleteById(id);
    }

    public Copy updateCopy(Long id, Copy copy) {
        if (!copyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        copy.setId(id);
        return copyRepository.save(copy);
    }
}
