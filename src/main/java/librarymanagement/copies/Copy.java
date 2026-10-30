package librarymanagement.copies;

import jakarta.persistence.*;
import librarymanagement.books.Book;
import librarymanagement.customers.Customer;

@Entity
@Table(name = "copies")
public class Copy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    private Book book;

    @ManyToOne
    private Customer customer;

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
