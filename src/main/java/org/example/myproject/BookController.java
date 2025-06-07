package org.example.myproject;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

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
    public Book addBook(@Valid @RequestBody Book book) {
        return bookRepository.save(book);
    }
}
