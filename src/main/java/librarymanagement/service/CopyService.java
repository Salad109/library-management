package librarymanagement.service;

import jakarta.transaction.Transactional;
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
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        return copy.get();
    }

    public Page<Copy> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        return copyRepository.findByBookIsbn(isbn, pageable);
    }

    public Page<Copy> getCopiesByBookIsbnAndStatus(String isbn, CopyStatus status, Pageable pageable) {
        return copyRepository.findByBookIsbnAndStatus(isbn, status, pageable);
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        return copyRepository.countByBookIsbnAndStatus(isbn, status);
    }

    @Transactional
    public Copy addCopy(Copy copy) {
        return copyRepository.save(copy);
    }

    @Transactional
    public Copy borrowCopy(Long copyId, Long customerId) {
        Optional<Copy> optionalCopy = copyRepository.findById(copyId);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + copyId);
        }
        Copy existingCopy = optionalCopy.get();
        if (existingCopy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy is not available for borrowing. Current status: " + existingCopy.getStatus());
        }

        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        Customer customer = optionalCustomer.get();

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.BORROWED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy returnCopy(Long copyId, Long customerId) {
        Optional<Copy> optionalCopy = copyRepository.findById(copyId);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + copyId);
        }
        Copy existingCopy = optionalCopy.get();
        if (existingCopy.getStatus() != CopyStatus.BORROWED) {
            throw new IllegalStateException("Copy is not currently borrowed. Current status: " + existingCopy.getStatus());
        }

        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        Customer customer = optionalCustomer.get();
        if (!existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException("This copy is not used by the specified customer. Current customer: " + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy markCopyAsLost(Long copyId, Long customerId) {
        Optional<Copy> optionalCopy = copyRepository.findById(copyId);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + copyId);
        }
        Copy existingCopy = optionalCopy.get();

        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        Customer customer = optionalCustomer.get();
        if (!existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException("Copy is not currently used by the specified customer. Current customer: " + existingCopy.getCustomer().getId());
        }

        existingCopy.setStatus(CopyStatus.LOST);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy reserveCopy(Long copyId, Long customerId) {
        Optional<Copy> optionalCopy = copyRepository.findById(copyId);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + copyId);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy is not available for reservation. Current status: " + optionalCopy.get().getStatus());
        }
        Copy existingCopy = optionalCopy.get();

        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        Customer customer = optionalCustomer.get();

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.RESERVED);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public Copy cancelCopyReservation(Long id, Long customerId) {
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        if (optionalCopy.get().getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException("Copy is not currently reserved. Current status: " + optionalCopy.get().getStatus());
        }
        Copy existingCopy = optionalCopy.get();

        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        Customer customer = optionalCustomer.get();
        if (!existingCopy.getCustomer().equals(customer)) {
            throw new IllegalStateException("Copy is not currently used by the specified customer. Current customer: " + existingCopy.getCustomer().getId());
        }

        existingCopy.setStatus(CopyStatus.AVAILABLE);
        return copyRepository.save(existingCopy);
    }

    @Transactional
    public void deleteCopy(Long id) {
        if (!copyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Copy not found with ID: " + id);
        }
        copyRepository.deleteById(id);
    }
}
