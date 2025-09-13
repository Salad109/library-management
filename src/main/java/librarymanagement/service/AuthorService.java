package librarymanagement.service;

import librarymanagement.constants.Messages;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.repository.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Cacheable(value = "authors", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    public Page<Author> getAllAuthors(Pageable pageable) {
        log.debug("Fetching all authors, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Author> authors = authorRepository.findAllByOrderByName(pageable);

        log.debug("Retrieved {} authors out of {} total", authors.getNumberOfElements(), authors.getTotalElements());

        return authors;
    }

    @Cacheable(value = "authors", key = "#name")
    public Author getAuthorByName(String name) {
        log.debug("Looking up author by name: {}", name);
        Optional<Author> author = authorRepository.findById(name);
        if (author.isEmpty()) {
            log.warn("Author not found with name: {}", name);
            throw new ResourceNotFoundException(Messages.AUTHOR_NOT_FOUND + name);
        }
        log.debug("Found author: '{}'", author.get().getName());
        return author.get();
    }
}
