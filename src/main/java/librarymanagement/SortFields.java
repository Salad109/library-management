package librarymanagement;

import librarymanagement.constants.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Which sorts a list accepts. Without the check a bad one fails inside the query as a 500.
 */
public final class SortFields {

    private static final Logger log = LoggerFactory.getLogger(SortFields.class);

    private SortFields() {
    }

    public static void requireAllowed(Pageable pageable, Set<String> allowedFields) {
        // "?sort=desc" names no field
        if (pageable.getSort().isUnsorted()) {
            log.warn("Attempted to sort without a field");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.SORT_FIELD_MISSING);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                log.warn("Attempted to sort by unsupported field: {}", order.getProperty());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        Messages.SORT_FIELD_UNSUPPORTED + order.getProperty());
            }
            // "ignorecase" wraps the column in lower(), which fails on a number or an enum
            if (order.isIgnoreCase()) {
                log.warn("Attempted to sort ignoring case by: {}", order.getProperty());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.SORT_IGNORE_CASE_UNSUPPORTED);
            }
        }
    }

    // Rows that tie on the requested sort can come back in a different order on each page, so end on a unique field
    public static Pageable withTiebreaker(Pageable pageable, String uniqueField) {
        Sort sort = pageable.getSort().and(Sort.by(uniqueField));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
