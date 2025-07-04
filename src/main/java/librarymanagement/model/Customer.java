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

    @NotNull(message = "First name cannot be blank")
    private String firstName;

    @NotNull(message = "Last name cannot be blank")
    private String lastName;

    @Column(unique = true)
    @Pattern(regexp = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$", message = "Email must be a valid email address")
    private String email;

    @OneToMany(mappedBy = "customer")
    private Set<Copy> copies;

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
