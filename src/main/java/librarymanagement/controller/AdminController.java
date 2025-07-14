package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.dto.BookUpdateRequest;
import librarymanagement.dto.CopyCreateRequest;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.Customer;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import librarymanagement.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
public class AdminController {

    private final BookService bookService;
    private final CopyService copyService;
    private final CustomerService customerService;

    public AdminController(BookService bookService, CopyService copyService, CustomerService customerService) {
        this.bookService = bookService;
        this.copyService = copyService;
        this.customerService = customerService;
    }

    // Book Management

    @PostMapping("/api/admin/books")
    public Book createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        return bookService.addBook(bookCreateRequest.toBook());
    }

    @PutMapping("/api/admin/books/{isbn}")
    public Book updateBook(@PathVariable String isbn, @Valid @RequestBody BookUpdateRequest bookUpdateRequest) {
        return bookService.updateBook(isbn, bookUpdateRequest.toBook());
    }

    @DeleteMapping("/api/admin/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
    }

    // Copy Management

    @GetMapping("/api/admin/copies")
    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyService.getAllCopies(pageable);
    }

    @GetMapping("/api/admin/copies/{id}")
    public Copy getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @GetMapping("/api/admin/copies/book/{isbn}")
    public Page<Copy> getCopiesByBookIsbn(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbn(isbn, pageable);
    }

    @PostMapping("/api/admin/copies")
    public List<Copy> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }

    // Customer Management

    @GetMapping("/api/admin/customers")
    public Page<Customer> getCustomers(Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/api/admin/customers/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @PutMapping("/api/admin/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }
}
