package librarymanagement.model;

import jakarta.persistence.*;

@Embeddable
public class Email {
    private String username;
    private String domain;

    public Email() {
    }

    public Email(String username, String domain) {
        this.username = username;
        this.domain = domain;
    }

    public Email(String email) {
        String[] parts = email.split("@");
        if (parts.length == 2) {
            this.username = parts[0];
            this.domain = parts[1];
        } else {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return username + "@" + domain;
    }
}
