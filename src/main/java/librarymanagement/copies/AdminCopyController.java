package librarymanagement.copies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.constants.Messages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Copy Operations", description = "Librarians can do CRUD operations on copies. Requires LIBRARIAN role")
public class AdminCopyController {

    private final CopyService copyService;

    public AdminCopyController(CopyService copyService) {
        this.copyService = copyService;
    }

    @Operation(summary = "Get copies, optionally filtered",
            description = "At most one filter. title and customerLastName match case-insensitive substrings; " +
                    "blank filters are ignored.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies found"),
            @ApiResponse(responseCode = "400", description = "More than one filter given"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies")
    public Page<CopyResponse> getCopies(@RequestParam(required = false) String isbn,
                                        @RequestParam(required = false) String title,
                                        @RequestParam(required = false) Long customerId,
                                        @RequestParam(required = false) String customerLastName,
                                        Pageable pageable) {
        isbn = StringUtils.hasText(isbn) ? isbn.strip() : null;
        title = StringUtils.hasText(title) ? title.strip() : null;
        customerLastName = StringUtils.hasText(customerLastName) ? customerLastName.strip() : null;
        if (Stream.of(isbn, title, customerId, customerLastName).filter(Objects::nonNull).count() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_SINGLE_FILTER);
        }

        if (isbn != null) {
            return copyService.getCopiesByBookIsbn(isbn, pageable);
        } else if (title != null) {
            return copyService.getCopiesByBookTitle(title, pageable);
        } else if (customerId != null) {
            return copyService.getCopiesByCustomerId(customerId, pageable);
        } else if (customerLastName != null) {
            return copyService.getCopiesByCustomerLastName(customerLastName, pageable);
        }
        return copyService.getAllCopies(pageable);
    }

    @Operation(summary = "Get a specific copy by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copy found"),
            @ApiResponse(responseCode = "404", description = "Copy not found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies/{id}")
    public CopyResponse getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @Operation(summary = "Add new copies of a book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Copies created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN")
    })
    @PostMapping("/api/admin/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CopyResponse> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }
}
