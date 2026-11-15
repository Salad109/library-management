package librarymanagement.copies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Copy Operations", description = "Librarians can do CRUD operations on copies. Requires LIBRARIAN role")
public class AdminCopyController {

    private final CopyService copyService;

    public AdminCopyController(CopyService copyService) {
        this.copyService = copyService;
    }

    @Operation(summary = "Get copies, optionally filtered",
            description = "Filters combine. title and customerLastName match case-insensitive substrings; " +
                    "blank filters are ignored.")
    @GetMapping("/api/admin/copies")
    public Page<CopyResponse> getCopies(@RequestParam(required = false) String isbn,
                                        @RequestParam(required = false) String title,
                                        @RequestParam(required = false) Long customerId,
                                        @RequestParam(required = false) String customerLastName,
                                        @ParameterObject @SortDefault("id") Pageable pageable) {
        return copyService.getCopies(blankToNull(isbn), blankToNull(title), customerId, blankToNull(customerLastName),
                pageable);
    }

    @GetMapping("/api/admin/copies/{id}")
    public CopyResponse getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @PostMapping("/api/admin/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CopyResponse> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }
}
