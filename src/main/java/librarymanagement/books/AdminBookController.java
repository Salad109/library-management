package librarymanagement.books;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PostMapping("/api/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        return bookService.addBook(bookCreateRequest);
    }

    @PutMapping("/api/admin/books/{isbn}")
    public BookResponse updateBook(@PathVariable String isbn, @Valid @RequestBody BookUpdateRequest bookUpdateRequest) {
        return bookService.updateBook(isbn, bookUpdateRequest);
    }

    @DeleteMapping("/api/admin/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
    }

}
