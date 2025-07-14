package librarymanagement.controller;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CopyController {

    private final CopyService copyService;

    public CopyController(CopyService copyService, BookService bookService) {
        this.copyService = copyService;
    }

    @GetMapping("/api/copies")
    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyService.getAllCopies(pageable);
    }

    @GetMapping("/api/copies/{id}")
    public Copy getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @GetMapping("/api/copies/book/{isbn}")
    public Page<Copy> getCopiesByBookIsbn(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbn(isbn, pageable);
    }

    @GetMapping("/api/copies/book/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }

    @DeleteMapping("/api/copies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCopy(@PathVariable Long id) {
        copyService.deleteCopy(id);
    }
}
