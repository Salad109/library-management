package librarymanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @Pattern(
            regexp = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
            message = "Email must be a valid email address"
    )
    private String email;

    @OneToMany(mappedBy = "customer")
    private Set<Copy> copies;

    public Customer() {
        copies = new LinkedHashSet<>();
    }

    public Customer(String firstName, String lastName, Set<Copy> copies) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.copies = copies != null ? new LinkedHashSet<>(copies) : new LinkedHashSet<>();
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
