package librarymanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import librarymanagement.constants.Messages;
import librarymanagement.model.Author;
import librarymanagement.model.Book;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record BookUpdateRequest(
        @NotBlank(message = Messages.BOOK_TITLE_VALIDATION_MESSAGE) String title,
        @Min(value = 1, message = Messages.BOOK_PUBLICATION_YEAR_VALIDATION_MESSAGE) Integer publicationYear,
        List<String> authorNames
) {
    public Book toBook() {
        Book book = new Book();
        book.setTitle(title);
        book.setPublicationYear(publicationYear);

        Set<Author> authors = new LinkedHashSet<>();
        for (String authorName : authorNames) {
            Author author = new Author();
            author.setName(authorName);
            authors.add(author);
        }
        book.setAuthors(authors);

        return book;
    }
}
