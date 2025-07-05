package librarymanagement.service;

import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Page<Author> getAllAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable);
    }

    public Author getAuthorByName(String name) {
        Optional<Author> author = authorRepository.findById(name);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("Author not found with name: " + name);
        }
        return author.get();
    }
}
