package librarymanagement.books;

import librarymanagement.authors.AuthorResponse;

import java.util.List;
import java.util.stream.Collectors;

public record BookResponse(
        String isbn,
        String title,
        List<AuthorResponse> authors,
        Integer publicationYear,
        Integer availableCopies
) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getAuthors().stream().map(AuthorResponse::from).toList(),
                book.getPublicationYear(),
                book.getAvailableCopies());
    }

    public String formattedAuthors() {
        return authors.stream()
                .map(AuthorResponse::name)
                .collect(Collectors.joining(", "));
    }
}
