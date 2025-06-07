package org.example.myproject;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @Min(value = 0, message = "Age must be positive")
    @Max(value = 999, message = "Age must be realistic")
    private int age;

    @NotBlank(message = "City cannot be empty")
    private String city;

    @Embedded
    private Email email;

    public User() {
    }

    // Constructor (don't include id - database will generate it)
    public User(String name, int age, String city, String emailString) {
        this.name = name;
        this.age = age;
        this.city = city;
        this.email = new Email(emailString);
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }
}

