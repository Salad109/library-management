package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.model.Author;
import librarymanagement.model.Book;
import librarymanagement.model.DuplicateIsbnException;
import librarymanagement.repository.AuthorRepository;
import librarymanagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @GetMapping("/api/books")
    public List<Book> getAllBooks(@RequestParam(required = false) Long id) {
        if (id == null) {
            return bookRepository.findAll();
        } else {
            Book book = bookRepository.findById(id).orElse(null);
            if (book != null) {
                return List.of(book);
            } else {
                return List.of();
            }
        }
    }

    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    public Book addBook(@Valid @RequestBody Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateIsbnException(book.getIsbn());
        }

        Set<Author> finalAuthors = new HashSet<>();
        for (Author incomingAuthor : book.getAuthors()){
            Author existingAuthor = authorRepository.findByName(incomingAuthor.getName());
            if (existingAuthor != null) {
                finalAuthors.add(existingAuthor); // if found, use existing author
            } else {
                finalAuthors.add(incomingAuthor); // else, add the new author
            }
        }
        book.setAuthors(finalAuthors);

        return bookRepository.save(book);
    }
}
