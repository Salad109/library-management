package librarymanagement.authors;

import librarymanagement.constants.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
public class AuthorService {
    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Cacheable(value = "authors", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public Page<AuthorResponse> getAllAuthors(Pageable pageable) {
        return authorRepository.findAllByOrderByName(pageable).map(AuthorResponse::from);
    }

    @Cacheable(value = "authors", key = "#name")
    @Transactional(readOnly = true)
    public AuthorResponse getAuthorByName(String name) {
        Author author = authorRepository.findById(name).orElseThrow(() -> {
            log.warn("Author not found with name: {}", name);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.AUTHOR_NOT_FOUND + name);
        });
        return AuthorResponse.from(author);
    }
}
