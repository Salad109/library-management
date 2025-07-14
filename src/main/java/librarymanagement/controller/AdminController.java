package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.dto.CopyCreateRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/api/admin/books")
    public void createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest) {
        bookService.addBook(bookCreateRequest.toBook());
    }

    @PostMapping("/api/admin/copies")
    public List<Copy> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }
}
