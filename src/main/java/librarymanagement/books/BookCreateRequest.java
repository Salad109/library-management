package librarymanagement.books;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BookCreateRequest(
        @NotNull @Pattern(regexp = Book.ISBN_REGEX, message = Book.ISBN_INVALID) String isbn,
        @NotBlank @Size(max = 150) String title,
        @NotNull Set<@NotBlank @Size(max = 100) String> authorNames,
        @Min(1) Integer publicationYear
) {
}
