package librarymanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE)
    private String firstName;

    @NotBlank(message = Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE)
    private String lastName;

    @Column(unique = true)
    @Pattern(regexp = Messages.CUSTOMER_EMAIL_REGEX,
            message = Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE)
    private String email;

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
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
