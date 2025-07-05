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
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + customer.getEmail());
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        Customer existingCustomer = optionalCustomer.get();
        String newEmail = customer.getEmail();

        // Check if new email is already taken by another customer
        Optional<Customer> potentialDuplicateCustomer = customerRepository.findByEmail(newEmail);
        if (potentialDuplicateCustomer.isPresent() && !potentialDuplicateCustomer.get().getId().equals(id)) {
            throw new DuplicateResourceException("Email already exists: " + customer.getEmail());
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
