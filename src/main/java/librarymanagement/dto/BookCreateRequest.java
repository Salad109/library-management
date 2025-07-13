package librarymanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;
import librarymanagement.model.Author;
import librarymanagement.model.Book;

import java.util.LinkedHashSet;
import java.util.Set;

public record BookCreateRequest(
        @Pattern(regexp = Messages.BOOK_ISBN_REGEX, message = Messages.BOOK_ISBN_VALIDATION_MESSAGE) String isbn,
        @NotBlank(message = Messages.BOOK_TITLE_VALIDATION_MESSAGE) String title,
        Set<@NotBlank String> authorNames,
        @Min(value = 1, message = Messages.BOOK_PUBLICATION_YEAR_VALIDATION_MESSAGE) Integer publicationYear

) {

    public Book toBook() {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setPublicationYear(publicationYear);

        Set<Author> authors = new LinkedHashSet<>();
        for (String authorName : authorNames) {
            authors.add(new Author(authorName));
        }

        book.setAuthors(authors);
        return book;
    }
}
