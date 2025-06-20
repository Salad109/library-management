package librarymanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "authors")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @ManyToMany(mappedBy = "authors")
    @JsonIgnore
    private Set<Book> books;

    public Author() {
        books = new LinkedHashSet<>();
    }

    public Author(String name) {
        this.name = name;
        this.books = new LinkedHashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Author) || getName() == null || ((Author) o).getName() == null) return false;
        return (getName().equals(((Author) o).getName()));
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Book> getBooks() {
        return books;
    }

    public void setBooks(Set<Book> books) {
        for (Book book : this.books) {
            book.getAuthors().remove(this);
        }
        this.books.clear();

        if (books != null) {
            for (Book book : books) {
                book.getAuthors().add(this);
                this.books.add(book);
            }
        }
    }
}
