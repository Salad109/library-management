package librarymanagement.controller;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Long id) {
        super("Book not found with ID: " + id);
    }
}
