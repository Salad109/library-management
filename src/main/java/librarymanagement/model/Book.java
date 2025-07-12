package librarymanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @Column(unique = true)
    @Pattern(
            regexp = Messages.BOOK_ISBN_REGEX,
            message = Messages.BOOK_ISBN_VALIDATION_MESSAGE
    )
    private String isbn;

    @Column(nullable = false)
    @NotBlank(message = Messages.BOOK_TITLE_VALIDATION_MESSAGE)
    private String title;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Author> authors;

    private Integer publicationYear;


    public Book() {
        authors = new LinkedHashSet<>();
    }

    public Book(String isbn, String title, Set<Author> authors, Integer publicationYear) {
        this.isbn = isbn;
        this.title = title;
        this.authors = (authors != null) ? new LinkedHashSet<>(authors) : new LinkedHashSet<>();
        this.publicationYear = publicationYear;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Book) || getIsbn() == null || ((Book) o).getIsbn() == null) return false;
        return (getIsbn().equals(((Book) o).getIsbn()));
    }

    @Override
    public int hashCode() {
        return getIsbn() != null ? getIsbn().hashCode() : 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        for (Author author : this.authors) {
            author.getBooks().remove(this);
        }
        this.authors.clear();

        if (authors != null) {
            for (Author author : authors) {
                author.getBooks().add(this);
                this.authors.add(author);
            }
        }
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

}