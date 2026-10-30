package librarymanagement.copies;

import librarymanagement.authors.AuthorResponse;
import librarymanagement.books.Book;
import librarymanagement.customers.CustomerResponse;

import java.util.List;

public record CopyResponse(Long id, BookSummary book, CustomerResponse customer, CopyStatus status) {

    public static CopyResponse from(Copy copy) {
        return new CopyResponse(
                copy.getId(),
                BookSummary.from(copy.getBook()),
                copy.getCustomer() != null ? CustomerResponse.from(copy.getCustomer()) : null,
                copy.getStatus());
    }

    // The copy's book without availableCopies
    public record BookSummary(String isbn, String title, List<AuthorResponse> authors, Integer publicationYear) {

        static BookSummary from(Book book) {
            return new BookSummary(
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthors().stream().map(AuthorResponse::from).toList(),
                    book.getPublicationYear());
        }
    }
}
