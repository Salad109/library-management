package librarymanagement.authors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "authors")
public class Author {
    @Id
    private String name;

    public Author() {
    }

    public Author(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Author(name=%s)".formatted(name);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Author) || getName() == null || ((Author) o).getName() == null) return false;
        return (getName().equals(((Author) o).getName()));
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
