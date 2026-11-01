package librarymanagement.copies;

import jakarta.validation.constraints.NotNull;
import librarymanagement.authors.AuthorResponse;
import librarymanagement.books.Book;
import librarymanagement.customers.CustomerResponse;

import java.util.List;

public record CopyResponse(@NotNull Long id,
                           @NotNull BookSummaryResponse book,
                           CustomerResponse customer,
                           @NotNull CopyStatus status) {

    public static CopyResponse from(Copy copy) {
        return new CopyResponse(
                copy.getId(),
                BookSummaryResponse.from(copy.getBook()),
                copy.getCustomer() != null ? CustomerResponse.from(copy.getCustomer()) : null,
                copy.getStatus());
    }

    // The copy's book without availableCopies
    public record BookSummaryResponse(@NotNull String isbn,
                                      @NotNull String title,
                                      @NotNull List<AuthorResponse> authors,
                                      Integer publicationYear) {

        static BookSummaryResponse from(Book book) {
            return new BookSummaryResponse(
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthors().stream().map(AuthorResponse::from).toList(),
                    book.getPublicationYear());
        }
    }
}
