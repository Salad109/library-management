package librarymanagement.model;

import jakarta.persistence.*;
import org.wildfly.common.annotation.NotNull;

@Entity
@Table(name = "copies")
public class Copy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Book book;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CopyStatus status; // "available", "reserved", "borrowed" or "lost"

    public Copy() {
    }

    public Copy(Book book, CopyStatus status) {
        this.book = book;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }
}
