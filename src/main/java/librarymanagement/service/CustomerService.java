package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Customer;
import librarymanagement.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Customer getCustomerById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        return customer.get();
    }

    public Customer addCustomer(Customer customer) {
        String email = customer.getEmail();

        if (email != null && !email.isBlank()) {
            Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
            if (existingCustomer.isPresent()) {
                throw new DuplicateResourceException("Email already exists: " + email);
            }
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Customer existingCustomer = getCustomerById(id);

        String newEmail = customer.getEmail();

        if (newEmail != null && !newEmail.isBlank()) {
            Optional<Customer> duplicateCustomer = customerRepository.findByEmail(newEmail);
            if (duplicateCustomer.isPresent() && !duplicateCustomer.get().getId().equals(id)) {
                throw new DuplicateResourceException("Email already exists: " + newEmail);
            }
        }

        existingCustomer.setFirstName(customer.getFirstName());
        existingCustomer.setLastName(customer.getLastName());
        existingCustomer.setEmail(customer.getEmail());
        return customerRepository.save(existingCustomer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}
