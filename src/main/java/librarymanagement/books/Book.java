package librarymanagement.books;

import jakarta.persistence.*;
import librarymanagement.authors.Author;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "books")
public class Book {
    // For @Pattern on the request records
    public static final String ISBN_REGEX = "^(?:\\d{9}[\\dX]|97[89]\\d{10})$";
    public static final String ISBN_INVALID = "ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979";

    @Id
    private String isbn;

    private String title;

    @BatchSize(size = 20)
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(joinColumns = @JoinColumn(name = "books_isbn"))
    private Set<Author> authors;

    private Integer publicationYear;

    @Formula("(SELECT COUNT(*) FROM copies c WHERE c.book_isbn = isbn AND c.status = 'AVAILABLE')")
    private Integer availableCopies = 0;

    @Version
    private Long version;

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
        this.authors.clear();
        this.authors.addAll(authors);
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

    public Integer getAvailableCopies() {
        return availableCopies;
    }

}