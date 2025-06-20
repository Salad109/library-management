package librarymanagement.controller;

public class DuplicateIsbnException extends RuntimeException {
    public DuplicateIsbnException(String message) {
        super("A book with this ISBN already exists: " + message);
    }
}
