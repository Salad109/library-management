package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Operations", description = "Librarians can manage books, copies, and customers. Requires LIBRARIAN role")
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

    @Operation(summary = "Add a new book to the catalog")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "409", description = "Book with this ISBN already exists")
    })
    @PostMapping("/api/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public Book createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        return bookService.addBook(bookCreateRequest.toBook());
    }

    @Operation(summary = "Update an existing book's details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN")
    })
    @PutMapping("/api/admin/books/{isbn}")
    public Book updateBook(@PathVariable String isbn, @Valid @RequestBody BookUpdateRequest bookUpdateRequest) {
        return bookService.updateBook(isbn, bookUpdateRequest.toBook());
    }

    @Operation(summary = "Remove a book from the catalog")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @DeleteMapping("/api/admin/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
    }

    // Copy Management

    @Operation(summary = "Get all copies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies")
    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyService.getAllCopies(pageable);
    }

    @Operation(summary = "Get a specific copy by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copy found"),
            @ApiResponse(responseCode = "404", description = "Copy not found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies/{id}")
    public Copy getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @Operation(summary = "Get all copies of a book by its ISBN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies/book/{isbn}")
    public Page<Copy> getCopiesByBookIsbn(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbn(isbn, pageable);
    }

    @Operation(summary = "Add new copies of a book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Copies created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN")
    })
    @PostMapping("/api/admin/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Copy> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }

    // Customer Management

    @Operation(summary = "Get all customers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/customers")
    public Page<Customer> getCustomers(Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @Operation(summary = "Get a specific customer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/customers/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @Operation(summary = "Update an existing customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Customer with this email already exists")
    })
    @PutMapping("/api/admin/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }
}
