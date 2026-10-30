package librarymanagement.books;

import jakarta.persistence.*;
import librarymanagement.authors.Author;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "books")
public class Book {
    @Id
    private String isbn;

    private String title;

    @BatchSize(size = 20)
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
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

    public String getFormattedAuthors() {
        return authors.stream()
                .map(Author::getName)
                .collect(Collectors.joining(", "));
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

    public Integer getAvailableCopies() {
        return availableCopies;
    }

}