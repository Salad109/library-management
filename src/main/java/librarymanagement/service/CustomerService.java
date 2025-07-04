package librarymanagement.service;

import jakarta.transaction.Transactional;
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

    public Page<Customer> findAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Customer findCustomerById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        return customer.get();
    }

    public Customer addCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        optionalCustomer.get().setFirstName(customer.getFirstName());
        optionalCustomer.get().setLastName(customer.getLastName());
        optionalCustomer.get().setEmail(customer.getEmail());
        return customerRepository.save(optionalCustomer.get());
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}
