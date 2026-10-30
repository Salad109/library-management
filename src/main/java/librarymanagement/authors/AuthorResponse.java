package librarymanagement.authors;

public record AuthorResponse(String name) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getName());
    }
}
