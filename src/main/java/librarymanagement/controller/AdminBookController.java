package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.dto.BookUpdateRequest;
import librarymanagement.model.Book;
import librarymanagement.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Book Operations", description = "Librarians can do CRUD operations on books. Requires LIBRARIAN role")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

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
        return bookService.addBook(bookCreateRequest);
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
        return bookService.updateBook(isbn, bookUpdateRequest);
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

}
