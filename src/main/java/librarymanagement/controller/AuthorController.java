package librarymanagement.controller;

import librarymanagement.model.Author;
import librarymanagement.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/api/authors")
    public Page<Author> getAuthors(Pageable pageable) {
        return authorService.getAllAuthors(pageable);
    }

    @GetMapping("/api/authors/{name}")
    public Author getAuthorByName(@PathVariable String name) {
        return authorService.getAuthorByName(name);
    }
}