package librarymanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import librarymanagement.constants.Messages;

@Entity
@Table(name = "copies")
public class Copy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(nullable = false)
    @NotNull(message = Messages.COPY_BOOK_VALIDATION_MESSAGE)
    @ManyToOne
    private Book book;

    @ManyToOne
    private Customer customer;

    @NotNull(message = Messages.COPY_STATUS_VALIDATION_MESSAGE)
    @Enumerated(EnumType.STRING)
    private CopyStatus status; // "available", "reserved", "borrowed" or "lost"

    public Copy() {
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
