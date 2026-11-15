package librarymanagement.customers;

import jakarta.persistence.*;
import librarymanagement.users.User;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String activationCodeHash;

    private Instant activationCodeExpiresAt;

    // Null until the customer registers. Lazy: copy lists show the customer without an email, so they never need the account
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Version
    private Long version;

    public Customer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getActivationCodeHash() {
        return activationCodeHash;
    }

    public void setActivationCodeHash(String activationCodeHash) {
        this.activationCodeHash = activationCodeHash;
    }

    public Instant getActivationCodeExpiresAt() {
        return activationCodeExpiresAt;
    }

    public void setActivationCodeExpiresAt(Instant activationCodeExpiresAt) {
        this.activationCodeExpiresAt = activationCodeExpiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
