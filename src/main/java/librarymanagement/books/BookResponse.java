package librarymanagement.books;

import jakarta.validation.constraints.NotNull;
import librarymanagement.authors.AuthorResponse;

import java.util.List;

public record BookResponse(
        @NotNull String isbn,
        @NotNull String title,
        @NotNull List<AuthorResponse> authors,
        Integer publicationYear,
        @NotNull Integer availableCopies
) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getAuthors().stream().map(AuthorResponse::from).toList(),
                book.getPublicationYear(),
                book.getAvailableCopies());
    }
}
