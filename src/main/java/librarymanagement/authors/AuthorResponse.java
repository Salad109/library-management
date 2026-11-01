package librarymanagement.authors;

import jakarta.validation.constraints.NotNull;
public record AuthorResponse(@NotNull String name) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getName());
    }
}
