package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.dto.BookUpdateRequest;
import librarymanagement.dto.CopyCreateRequest;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
public class AdminController {

    private final BookService bookService;
    private final CopyService copyService;

    public AdminController(BookService bookService, CopyService copyService) {
        this.bookService = bookService;
        this.copyService = copyService;
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

    @DeleteMapping("/api/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
    }

    // Copy Management

    @PostMapping("/api/admin/copies")
    public List<Copy> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }
}
