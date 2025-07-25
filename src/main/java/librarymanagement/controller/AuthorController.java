package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import librarymanagement.model.Author;
import librarymanagement.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Public Browsing", description = "No auth required")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Operation(summary = "Get all authors")
    @ApiResponse(responseCode = "200", description = "Authors retrieved successfully")
    @GetMapping("/api/authors")
    public Page<Author> getAuthors(Pageable pageable) {
        return authorService.getAllAuthors(pageable);
    }

    @Operation(summary = "Get author by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author found"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @GetMapping("/api/authors/{name}")
    public Author getAuthorByName(@PathVariable String name) {
        return authorService.getAuthorByName(name);
    }
}