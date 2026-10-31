package librarymanagement.customers;

import jakarta.persistence.*;
import librarymanagement.copies.Copy;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String activationCodeHash;

    private Instant activationCodeExpiresAt;

    @OneToMany(mappedBy = "customer")
    private Set<Copy> copies;

    @Version
    private Long version;

    public Customer() {
        copies = new LinkedHashSet<>();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Set<Copy> getCopies() {
        return copies;
    }

    public void setCopies(Set<Copy> copies) {
        for (Copy copy : this.copies) {
            copy.setCustomer(null);
        }
        this.copies.clear();

        if (copies != null) {
            for (Copy copy : copies) {
                copy.setCustomer(this);
            }
            this.copies = new LinkedHashSet<>(copies);
        } else {
            this.copies = new LinkedHashSet<>();
        }
    }

}
