package librarymanagement.copies;

import librarymanagement.books.BookResponse;
import librarymanagement.customers.CustomerResponse;

public record CopyResponse(Long id, BookResponse book, CustomerResponse customer, CopyStatus status) {

    public static CopyResponse from(Copy copy) {
        return new CopyResponse(
                copy.getId(),
                BookResponse.from(copy.getBook()),
                copy.getCustomer() != null ? CustomerResponse.from(copy.getCustomer()) : null,
                copy.getStatus());
    }
}
