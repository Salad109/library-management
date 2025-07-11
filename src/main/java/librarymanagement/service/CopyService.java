package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.model.Customer;
import librarymanagement.repository.CopyRepository;
import librarymanagement.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CopyService {

    private final CopyRepository copyRepository;
    private final CustomerRepository customerRepository;

    public CopyService(CopyRepository copyRepository, CustomerRepository customerRepository) {
        this.copyRepository = copyRepository;
        this.customerRepository = customerRepository;
    }

    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyRepository.findAll(pageable);
    }

    public Copy getCopyById(Long id) {
        Optional<Copy> copy = copyRepository.findById(id);
        if (copy.isEmpty()) {
            throw new ResourceNotFoundException(Messages.COPY_NOT_FOUND + id);
        }
        return copy.get();
    }

    public Page<Copy> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        return copyRepository.findByBookIsbn(isbn, pageable);
    }

    public Page<Copy> getCopiesByCustomerId(Long customerId, Pageable pageable) {
        return copyRepository.findByCustomerId(customerId, pageable);
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.countByBookIsbnAndStatus(isbn, status);
    }

    @Transactional
    public Copy addCopy(Copy copy) {
        return copyRepository.save(copy);
    }

    @Transactional
    public Copy borrowAnyAvailableCopy(String isbn, Long customerId) {
        Page<Copy> availableCopies = copyRepository.findByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE, Pageable.unpaged());
        if (availableCopies.isEmpty()) {
            throw new ResourceNotFoundException(Messages.COPY_NO_AVAILABLE + isbn);
        }

        Copy copyToBorrow = availableCopies.getContent().get(0); // Borrow the first available copy
        return borrowCopy(copyToBorrow.getId(), customerId);
    }

    @Transactional
    public Copy borrowCopy(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException(Messages.COPY_UNAVAILABLE_FOR_BORROWING + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.BORROWED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy returnCopy(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.BORROWED) {
            throw new IllegalStateException(Messages.COPY_NOT_BORROWED + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy markCopyAsLost(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);

        Customer customer = getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setStatus(CopyStatus.LOST);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy reserveAnyAvailableCopy(String isbn, Long customerId) {
        Page<Copy> availableCopies = copyRepository.findByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE, Pageable.unpaged());
        if (availableCopies.isEmpty()) {
            throw new ResourceNotFoundException(Messages.COPY_NO_AVAILABLE + isbn);
        }

        Copy copyToReserve = availableCopies.getContent().get(0); // Reserve the first available copy
        return reserveCopy(copyToReserve.getId(), customerId);
    }

    @Transactional
    public Copy reserveCopy(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException(Messages.COPY_UNAVAILABLE_FOR_RESERVATION + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.RESERVED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy cancelCopyReservation(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException(Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setStatus(CopyStatus.AVAILABLE);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy checkoutReservedCopy(Long copyId) {
        Copy existingCopy = getCopyOrThrow(copyId);

        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException(Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
        }

        existingCopy.setStatus(CopyStatus.BORROWED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public void deleteCopy(Long id) {
        if (!copyRepository.existsById(id)) {
            throw new ResourceNotFoundException(Messages.COPY_NOT_FOUND + id);
        }
        copyRepository.deleteById(id);
    }

    // Helpers

    public Copy getCopyOrThrow(Long id) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException(Messages.COPY_NOT_FOUND + id);
        }
        return optionalCopy.get();
    }

    public Customer getCustomerOrThrow(Long customerId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException(Messages.CUSTOMER_NOT_FOUND + customerId);
        }
        return optionalCustomer.get();
    }
}
