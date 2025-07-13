package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.service.BookService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
public class AdminController {

    private final BookService bookService;

    public AdminController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/api/admin/books")
    public void createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        bookService.addBook(bookCreateRequest.toBook());
    }
}
