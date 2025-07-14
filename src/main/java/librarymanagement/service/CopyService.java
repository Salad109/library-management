package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.model.Customer;
import librarymanagement.repository.BookRepository;
import librarymanagement.repository.CopyRepository;
import librarymanagement.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    private final CopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    public CopyService(CopyRepository copyRepository, BookRepository bookRepository, CustomerRepository customerRepository) {
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
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

    public Page<Copy> getReservationsByCustomerId(Long customerId, Pageable pageable) {
        return copyRepository.findByCustomerIdAndStatus(customerId, CopyStatus.RESERVED, pageable);
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.countByBookIsbnAndStatus(isbn, status);
    }

    @Transactional
    public List<Copy> addCopies(String isbn, int quantity) {
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn));

        List<Copy> copies = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Copy copy = new Copy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            copies.add(copy);
        }
        return copyRepository.saveAll(copies);
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
    public Copy markLost(Long copyId) {
        Copy existingCopy = getCopyOrThrow(copyId);

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
        Customer customer = getCustomerOrThrow(customerId);

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.RESERVED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy cancelReservation(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException(Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
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
    public Copy checkout(Long copyId, Long customerId) {
        Copy copy = getCopyOrThrow(copyId);
        Customer customer = getCustomerOrThrow(customerId);

        // Reserved copy checkout
        if (copy.getStatus() == CopyStatus.RESERVED) {
            return checkoutReservedCopy(copy, customer);
        }

        // Direct checkout
        if (copy.getStatus() == CopyStatus.AVAILABLE) {
            return checkoutAvailableCopy(copy, customer);
        }

        throw new IllegalStateException(Messages.COPY_UNAVAILABLE_FOR_CHECKOUT + copy.getStatus());
    }

    private Copy checkoutReservedCopy(Copy copy, Customer customer) {
        if (!copy.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException(Messages.COPY_RESERVED_FOR_ANOTHER_CUSTOMER + copy.getCustomer().getId());
        }
        copy.setStatus(CopyStatus.BORROWED);
        return copyRepository.save(copy);
    }

    private Copy checkoutAvailableCopy(Copy copy, Customer customer) {
        copy.setCustomer(customer);
        copy.setStatus(CopyStatus.BORROWED);
        return copyRepository.save(copy);
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
