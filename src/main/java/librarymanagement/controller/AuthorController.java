package librarymanagement.controller;

import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.repository.AuthorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthorController {

    private final AuthorRepository authorRepository;

    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/api/authors")
    public List<Author> getAuthors() {
        return authorRepository.findAll();
    }

    @GetMapping("api/authors/{name}")
    public Author getAuthorByName(@PathVariable String name) {
        Author author = authorRepository.findById(name).orElse(null);
        if (author == null) {
            throw new ResourceNotFoundException("Author not found with name: " + name);
        }
        return author;
    }

    @GetMapping("/api/authors/search")
    public List<Author> searchAuthors(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return authorRepository.findAll();
        } else {
            return authorRepository.findByNameContainingIgnoreCase(name);
        }
    }
}