package librarymanagement.controller;

import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthorController {

    private final AuthorRepository authorRepository;

    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/api/authors")
    public Page<Author> getAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable);
    }

    @GetMapping("/api/authors/{name}")
    public Author getAuthorByName(@PathVariable String name) {
        Optional<Author> author = authorRepository.findById(name);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("Author not found with name: " + name);
        }
        return author.get();
    }
}