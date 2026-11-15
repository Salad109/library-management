package librarymanagement.books;

import jakarta.validation.constraints.*;

import java.util.Set;

public record BookCreateRequest(
        @NotNull @Pattern(regexp = Book.ISBN_REGEX, message = Book.ISBN_INVALID) String isbn,
        @NotBlank @Size(max = 150) String title,
        @NotNull Set<@NotBlank @Size(max = 100) String> authorNames,
        @Min(1) Integer publicationYear
) {
}
